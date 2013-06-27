(ns ^{:doc "A concrete and symbolic metacircular evaluator"
      :author "Zenna Tavares"}
  relax.evalcs
  (:use relax.env))

; Forward declarations
(declare evalcs)

;Error
(defn error
  [error-text]
  (throw (Throwable. error-text)))

; Application abstractions
(defn application? [exp] (list? exp))
(defn operator [exp] (first exp))
(defn operands [exp] (rest exp))
(defn no-operands? [ops] (empty? ops))
(defn first-operand [ops] (first ops))
(defn rest-operands [ops] (rest ops))

(defn list-of-values
  "Produce the list of arguments to which the procedure is to be applied"
  [exps env]
  (if (no-operands? exps)
      '()
      (cons (evalcs (first-operand exps) env)
            (list-of-values (rest-operands exps) env))))

; (defn eval-sequence
;   "Used by apply to evaluate the sequence of expressions in a proecdure body.
;    Used by eval to evlauate sequence of expressions in begin expr"
;   [exps env]
;   (cond
;     (last-exp? exps)
;     (eval (first-exp exps) env)

;     :else
;     ; NOT SURE THIS IS VALID CLOJURE
;     (eval (first-exp exps) env)
;     (eval-sequence (rest-exps exps) env)))

(defn eval-assignment
  [exp env]
  "Handles assignments to variables.

  It calls eval to find the value to be assigned and transmits the variable
  and the resulting value to set-variable-value! to be installed in
  the designated environment."
  (set-variable-value! (assignment-variable exp)
                       (evalcs (assignment-value exp) env)
                       env)
  ’ok)

(defn
  eval-definition
  [exp env]
  (define-variable! (definition-variable exp)
                    (evalcs (definition-value exp) env)
                    env)
  ’ok)

;;
(defn self-evaluating?
  [exp]
  (cond (number? exp) true
        (string? exp) true
        :else false))
(defn tagged-list?
  [exp tag]
  (if (pair? exp)
      (eq? (first exp) tag)
      false))
(defn variable? [exp] (symbol? exp))
(defn quoted? [exp] (tagged-list? exp ’quote))
(defn text-of-quotation [exp] (rest exp))

; necessary? our predicates should be pure
(defn assignment? [exp] (tagged-list? exp ’set!))
(defn assignment-variable [exp] (nth exp 1))
(defn assignment-value [exp] (nth exp 2))

; Definitions
(defn definition? [exp] (tagged-list? exp ’define))
(defn definition-variable [exp]
  (if (symbol? (nth exp 1))
      (nth exp 1)
      (first (nth exp 1))))
(defn definition-value [exp]
  (if (symbol? (nth exp 1)
      (nth exp 2)
      (make-lambda (rest (nth exp 1))
                   (rest (rest exp))))))

; Conditionals
(defn if? [exp] (tagged-list? exp ’if))
(defn if-predicate [exp] (nth exp 1))
(defn if-consequent [exp] (first (rest (rest exp)))
(defn if-alternative [exp]
  (if (not (empty? (rest (rest (rest exp)))))
      (first (rest (rest (rest exp))))
      ’false))

(defn eval-if
  "Evaluate a predicate part of an if expression in the given environment.
   If the result is true, eval-if evaluates the consequent, otherwise it evaluates
   the alternative"
   [exp env]
   (if (true? (evalcs (if-predicate exp) env))
       (evalcs (if-consequent exp) env)
       (evalcs (if-alternative exp) env)))

;
(defn true? [x] (not (eq? x false)))
(defn false? [x] (eq x false))

; Lambda
(defn lambda? [exp] (tagged-list? exp ’lambda))
(defn lambda-parameters [exp] (nth exp 1))
(defn lambda-body [exp] (rest (rest exp)))
(defn make-lambda [parameters body]
  (list 'lambda (list parameters body)))

; Procedures
(defn make-procedure [body env]
  (list 'procedure parameters body env))
(defn compound-procedure? [p]
  (tagged-list? p 'procedure))
(defn procedure-parameters [proc] (nth proc 1))
(defn procedure-body [proc] (nth proc 2))
(defn procedure-environment [proc] (nth proc 3)

(defn primitive-procedure?
  [proc]
  (tagged-list? proc 'primitive))
(defn primitive-implementation
  [proc]
  (nth proc 1))

(def primitive-prodecures
  (list (list '+ +)
        (list '- -)))

(def primitive-procedure-names
  (map first primitive-prodecures))

(def primitive-procedure-objects
  (map #(list 'primitive (nth % 1)) primitive-prodecures))

(defn apply-primitive-procedure
  [proc args]
  (apply proc args))

(defn applysc
  "Apply"
  [procedure arguments]
  (cond
    (primitive-procedure? procedure)
    (apply-primitive-procedure procedure arguments)))

    ; (compound-procedure? procedure)
    ; (eval-sequence)
    ;   (procedure-body procedure)
    ;   (extend-environment
    ;     (procedure-parameters procedure)
    ;     arguments
    ;     (procedure-evnironment procedure))

    :else
      (error "unknown procedure type:apply procedure")))

(defn evalcs
  "Evaluate an expression"
  [exp env]
  (cond
    (self-evaluating? exp) exp
    (variable? exp) (look-up-variable-value exp env)
    (assignment? exp) (eval-assignment exp env)
    (definition? exp) (eval-definition exp env)
    (if? exp) (eval-if expr env)
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
      (error "Unknown expression typoe: EVAL" exp))