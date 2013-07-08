(ns ^{:doc "A concrete and symbolic metacircular evaluator"
      :author "Zenna Tavares"}
  relax.symbolic
  (:use relax.env)
  (:use relax.common)
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

;; Symbolic value abstractions
(defn symbolic? [val]
  "is this value symbolic?"
  (tagged-list? val 'symbolic))

(defn make-symbolic [val]
  "Create a symbolic version of a val"
  (list 'symbolic val))

(defn define-symbolic! [var env]
  "Creates a symbolic variable unconstrained"
  (define-variable! var (list 'symbolic var) env))

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

;; Mutlivalue abstractions
(defn make-multivalue
  [& values]
  "Make a multivalue"
  (list 'multivalue (vec values)))

(defn multivalue? [val]
  "Construct a multivalue: a compound value where the variable has many possible values"
  (tagged-list? val 'multivalue))

(defn multivalues [val]
  (if (multivalue? val)
      (nth val 1)
      [val]))

(defn make-merge-multivalue
  "Make a multivalue but if any of the values are already multivalues then merge them in
  (as opposed to creating nested multivalues)"
  [& values]
  (if (not-any? multivalue? values)
      (apply make-multivalue values)
      (reduce 
        #(cond
          (and (multivalue? %1) (multivalue? %2))
          (list 'multivalue (vec (concat (multivalues %1) (multivalues %2))))

          (multivalue? %1)
          (list 'multivalue (conj (multivalues %1) %2))

          (multivalue? %2)
          (list 'multivalue (conj (multivalues %2) %1))

          :else
          (make-multivalue %1 %2))
        values)))

; TODO support nested multivalues
(defn multify
  "Multify takes a function that does not support multivalues
   and returns one that does"
  [f & args]
  (cond
    (not-any? multivalue? args)
    (apply f args)

    :else
    (apply make-multivalue
      (map #(apply f %) 
            (apply combo/cartesian-product (map multivalues args))))))

(defn multify-apply
  [f args]
  (cond
    (not-any? multivalue? args)
    (apply f args)

    :else
    (apply make-multivalue
      (map #(apply f %) 
            (apply combo/cartesian-product (map multivalues args))))))

; TODO
(defn feasible? [cond env]
  true)

;; Conditional value abstractions

; Condition abstractions
(defn conditional-value?
  "A conditioned value has the form (condition multivalue)
   where each element of multivalue (possible-value) has the form
   ('possible-value value_i [condition_1, ..., condition_n])"
  [exp]
  (tagged-list? exp 'conditional-value))

(defn possible-value?
  [exp]
  (tagged-list? exp 'possible-value))

(defn all-conditional-values
  [cond-val]
  (nth cond-val 1))


(defn value-conditions
  "Get the conditions of a conditioned value"
  [val]
  (nth val 2))

(defn merge-conditions
  [possible-values]
  (reduce #(vec (concat %1 %2))
           (map value-conditions possible-values)))

(defn conditional-value
  "Get the conditions of a conditioned value"
  [val]
  (nth val 1))

(defn apply-condition
  [val new-conditions]
  (list 'conditioned-value val new-conditions))

; ; TODO
; (defn update-condition
;   "Add a condition to already conditioned value"
;   [val new-conditions]
;   (replace-in-list val 2 (vec concat (conditions val) new-conditions)))

(defn make-conditional-value
  ""
  [& args]
  {:pre [(even? (count args))]}
  (l x (apply make-multivalue (map (fn [x] `(~'possible-value ~@x)) (partition 2 args)))
  `(~'conditional-value ~x)))

; TODO support nested multivalues
(defn handle-conditional
  "Multify takes a function that does not support multivalues
   and returns one that does"
  [f & args]
  ; (println "args " args)
  (cond
    (not-any? conditional-value? args)
    [(apply f args)]

    :else
    (let [x (apply combo/cartesian-product 
                   (map #(if (conditional-value? %)
                             (multivalues (all-conditional-values %))
                             [%])
                          args))]
      (apply make-conditional-value
        (reduce concat 
          (map (fn [arg-list]
                 [(apply f (map #(if (possible-value? %) 
                                         (conditional-value %)
                                          %)
                                    arg-list))
                  (merge-conditions (filter possible-value? arg-list))])
                x))))))

(defn -main []
  (let [x1 (make-conditional-value 3 ['godisgood] 4 ['notsogood])
        x2 (make-conditional-value 10 ['satanisbad] 100 ['ilikethefire])]
    (handle-conditional + 1 2 x1 x2)))

; (defn add-conditions
;   "adds conditions to a value, concatenates conditions if already present"
;   [cond-value value new-conditions]
;   (cond
;     (conditional-value? val)
;     (update-condition val new-conditions)

;     (conditional-value? new-conditions)
;     (error "conditioned conditions not supported" new-conditions)

;     :else
;     (apply-condition val new-conditions)))