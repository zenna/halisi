(ns ^{:doc "A concrete and symbolic metacircular evaluator"
      :author "Zenna Tavares"}
  relax.symbolic
  (:use relax.env)
  (:use relax.common))

(defn symbolic? [val]
  "is this value symbolic?"
  (tagged-list? val 'symbolic))

(defn make-symbolic [val]
  "Create a symbolic version of a val"
  (list 'symbolic val))

(defn define-symbolic! [var val env]
  "Creates a symbolic variable unconstrained"
  (define-variable! var (list 'symbolic nil) env))

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

(defn negate
  "negate a predicate: i.e., an inequality.
   (negate '(< x 3)) => (>= x 3)"
  [pred]
  (condp = (operator pred)
    '<  (cons '>= (rest pred))
    '<= (cons '> (rest pred))
    '>  (cons '<= (rest pred))
    '>= (cons '< (rest pred))
    (list 'not pred)))

; Mutlivalue abstractions
(defn make-multivalue
  [values]
  "Make a multivalue"
  (list 'multivalue values))

(defn multivalue? [val]
  (tagged-list? val 'multivalue))

; TODO
(defn multify
  "Multify takes a function that does not support multivalues
   and returns one that does"
  [f args]
  (make-conditon val1 val2))))

  (not (and (multivalue? val1)
            (mutlivalue? val2)))
  (vec val1 val2)

  (mutlivalue? val1)

(defn feasible? [cond env]
  true)

; Condition abstractions
(defn conditioned-value?
  "A conditioned value has the form
  (condition value condition)"
  [exp]
  (tagged-list exp 'condition))

(defn condition-value
  [val condition]
  (list 'condition val [condition]))

(defn update-condition
  [val condition]
  (update-list #(conj % condition) val))

(defn add-condition
  ""
  [val condition]
  (cond
    (conditioned-value? val)
    (update-condition val condition)

    :else
    (condition-value val condition)))