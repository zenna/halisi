(ns ^{:doc "A concrete and symbolic metacircular evaluator"
      :author "Zenna Tavares"}
  relax.symbolic
  (:use relax.env)
  (:use relax.common))

(defn symbolic? [val]
  "is this value symbolic?"
  tagged-list? val 'symbolic)

(defn define-symbolic [var val env]
  "Creates a symbolic variable unconstrained"
  (define-variable! (list 'symbolic var) '() env))

; Abstractions for concrete-symbolic hybrid datastructure
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

(def exp '(if (> x1 0.5)
    (and (> x2 0.7) (< x2 0.9))
    (and (> x1 0.1) (< x1 0.4)
         (> x2 0.3) (< x2 0.5)
         (or (< x3 0.1) (> x3 0.9)))))

(if (> x1 (+ 3 2))
  (if (> x2 0.7)
    (if (< x2 0.9)
        true
        false)
    false)
  (if (> x1 0.1)
      (if (< x1 0.4)
          (if (> x2 0.3)
              (if (< x2 0.5) 
                  (if (if (< x3 0.1)
                          true
                          (if (> x3 0.9)
                              true
                              false))
                      true
                      false)
                  false)
              false)
          false)
      false))

(if (> x1 (+ 3 2))
  (if (> x2 0.7)
    (if (< x2 0.9)
        true
        false)
    false)
  false)

(if (> x1 (+ 3 2))
  (if (> x2 0.7)
      true
      false)
  true)


[[true false] false]

; assume that eval-symb will return something like
; (> x1 5)
; (consistent? (> x1 5) (x -)))

1. Do symbolic evaluation
2. Check for consistency
3. Do merging
4. Get variable values back

(defn eval-if-symb
  "Symbolically evaluate if expression"
  [exp env]
  (filter #(true? (concrete %))
    (let [symb-val (eval-symb (if-condition exp) env)]
    (list
      (if (consistent? symb-val env)
          (evalcs (if-consequent exp) (merge-env env symb-val))
          'inconsistent)
      (if (consistent? (negate (if-condition exp)) env)
          (evalcs (if-alternative exp) env)
          'inconsistent)))))

(defn eval-symb
  "Evaluate an expression symbolically
   I want to find path constraints that lead to true"
  [exp env]
  (cond
    (self-evaluating? exp) exp
    (variable? exp) (lookup-variable-value exp env)
    (assignment? exp) (eval-assignment exp env)
    (definition? exp) (eval-definition exp env)
    (if? exp) (eval-if exp env)
    ; (lambda? exp)
    ;   (make-procedure (lambda-parameters exp)
    ;                   (lambda-body exp)
    ;                   env)
    ; (begin? exp)
    ;   (eval-sequence (begin-actions exp) env)
    (application? exp)
      (applycs (evalcs (operator exp) env)
             (list-of-values (operands exp) env))
    :else
      (error "Unknown expression type: EVAL" exp)))