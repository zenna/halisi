(ns ^{:doc "A concrete and symbolic metacircular evaluator"
      :author "Zenna Tavares"}
  sigma.symbolic
  (:use sigma.env)
  (:use sigma.common)
  (:use sigma.linprog)
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

;; Symbolic value abstractions
(defn symbolic? [val]
  "is this value symbolic?"
  (tagged-list? val 'sym))

(defn make-symbolic [val]
  "Create a symbolic version of a val"
  (list 'sym val))

(defn define-symbolic! [var env]
  "Creates a symbolic variable unconstrained"
  (define-variable! var (list 'sym var) env))

(defn symbolic-value [val]
  (nth val 1))

;; Abstractions for concrete-symbolic hybrid datastructure
(defn make-hybrid
  "Construct a hybrid forom a concrete and symbolic value"
  [conc-part symb-part]
  (list 'hybrid conc-part symb-part))

(defn hybrid? [val]
  (tagged-list? val 'hybrid))

(defn concrete-part [val]
  "Get concrete part of value. Which is just the value if it is not hybrid"
  (if (hybrid? val)
      (nth val 1)
      val))

(defn symbolic-part [val]
  "Get symbolic part of value. Which is just the value if it is not hybrid"
  (if (hybrid? val)
      (nth val 2)
      val))

(declare andor-to-if)

;; Program transformations TODO: Should probably be in its own namespace
(defn handle-and [ops]
  "Converts sub expression  containing OR to IF form, (or a b c)
  (if a true (if b true (if c true false)))"
  (cond
    (= 1 (count ops))
    (list 'if (andor-to-if (first-operand ops))
               true
               false)

    :else
    (list 'if (andor-to-if (first-operand ops))
               (handle-and (rest-operands ops)) false)))

; TODO, I can rewrite handle functions such that they work independently of
; andor-to-if.  That would be better for modularity.
(defn handle-or [ops]
  "Converts sub expression containing AND to IF form, (AND a b c)
  (if a true (if b true (if c true false)))"
  (cond
    (= 1 (count ops))
    (list 'if (andor-to-if (first-operand ops))
               true
               false)

    :else
    (list 'if (andor-to-if (first-operand ops))
              true
              (handle-or (rest-operands ops)))))

(defn andor-to-if [exp]
  "Convert ands and ors to if statements"
  (cond
    (tagged-list? exp 'and)
    (handle-and (operands exp))

    (tagged-list? exp 'or)
    (handle-or (operands exp))

    (list? exp)
    (map andor-to-if exp)

    :else
    exp))

(defn negate
  "negate a predicate: i.e., an inequality.
   (negate '(< x 3)) => (>= x 3)"
  [pred]
  (condp = (operator pred)
    '<  (replace-in-list pred 0 '>=)
    '<= (replace-in-list pred 0 '>)
    '>  (replace-in-list pred 0 '<=)
    '>= (replace-in-list pred 0 '<)
    (list 'not pred)))

;; Symbol manipulations (as if there were anything else)
(defn decompose-binary-exp
  "Takes a binary exp involving a symbol and a concrete number
   and extracts them into a map.

   Useful to find the symbol and/or number when an expression could be
   (+ x 2) or (+ 2 x) for instance"
  [ineq]
  (cond
    (symbolic? (nth ineq 1))
    {:num (nth ineq 2) :symb (nth ineq 1)}

    (symbolic? (nth ineq 2))
    {:num (nth ineq 1) :symb (nth ineq 2)}

    :else
    (error "one of the values in inequality must be symbolic")))

(defn unsymbolise
  [formula]
  "Remove symbols from something like this:
  (<= (symbolic (+ (symbolic (* -4 (symbolic x1))) (symbolic x2))) 10)"
  (map 
    #(let [value (if (symbolic? %)
                     (symbolic-value %)
                     %)]
      (if (coll? value)
          (unsymbolise value)
          value))
    formula))

(defn conjoin
  "Conjoin an expression symbolically"
  [& exprs]
  `(~'and ~@exprs))

(defn extract-vars
  "Extract the variables from a symbolci expr
   '(> (+ (* 2 (sym x1))
              (- (sym x2))
              (sym x2))
            5))
    --> #{x1 x2}"
  [constraint]
  (reduce
    (fn [variables sub-exp]
      ; (println "vars" variables "sub-exp" sub-exp)
      (cond
        (symbolic? sub-exp)
        (if (coll? (symbolic-value sub-exp))
            (reduce conj variables
                         (extract-vars (symbolic-value sub-exp)))
            (conj variables (symbolic-value sub-exp)))

        (coll? sub-exp)
        (reduce conj variables (extract-vars sub-exp))

        :else
        variables))
    #{}
    constraint))

; (sym (> (sym (+ (sym y) (sym (- (sym x))) -5)) 0))
; (sym (< (sym (+ (sym y) (sym (* -0.5 (sym x))) -4)) 0))
; (sym (>= (sym (+ (sym y5) (sym (* -1 (sym y4))))) 0))

(defn ineq-as-matrix
  "Takes an inequality expression, e.g. (> x 2) and converts it
   into a matrix for use with the linear programming solver"
  [exp vars]
  (let [var-id (zipmap vars (range (count vars)))
        exp (symbolic-value exp)
        second-arg (symbolic-value (second exp))
        add-sub (if (coll? second-arg)
                    (eval (operator second-arg))
                    +)
        arguments (if (coll? second-arg)
                      (rest second-arg)
                      [(make-symbolic second-arg)])]
    [(pass
        (fn [term row]
          (cond
            (tagged-list? (symbolic-value term) '*)
            (let [{num :num symb :symb} (decompose-binary-exp (symbolic-value term))]
              (assoc row (var-id (symbolic-value symb)) (* (add-sub 1) num)))

            (symbolic? term)
            (assoc row (var-id (symbolic-value term)) (add-sub 1))

            :else
            (error "UNKOWN TERM" term)))

        (vec (zeros (count vars)))
        arguments)
    
    (range 1 (inc (count vars)))
    (operator exp)
    (last exp)]))

(defn feasible? [conjun-terms env]
  "Is this set of terms feasible, i.e. consistent
   e.g. (and (> x 2) (< x 0)) is not feasible
   since the conjunction can be satisfied by no points"
  (let [vars (vec (merge-sets (map extract-vars conjun-terms)))
        ; pvar (println "VARS IS " vars)
        constraints (mapv #(ineq-as-matrix % vars) conjun-terms)
        classification (classify-ineqs constraints vars)]
    ; (println "Type is classification" classification)
    ; (println (count conjun-terms))
    (not= classification 'infeasible)))