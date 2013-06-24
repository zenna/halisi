(ns ^{:doc "A metacircular evaluator"
      :author "Zenna Tavares"}
  avalance.metacircular)

; Evaluate until end

(defn list-of-values
  "Produce the list of arguments to which the procedure is to be applied"
  [exps env]
  (if (no-operands? exps)
      '()
      (cons (eval (first-operand exps) env)
            (list-of-values (rest-operands exps) env))))

(defn eval-if
  "Evaluate a predicate part of an if expression in the given environment.
   If the result is true, eval-if evaluates the consequent, otherwise it evaluates
   the alternative"
   [exp env]
   (if (true? (eval (if-predicate exp) env))
       (eval (if-consequent exp) env)
       (eval (if-alternative exp) env)))

(defn eval-sequence
  "Used by apply to evaluate the sequence of expressions in a proecdure body.
   Used by eval to evlauate sequence of expressions in begin expr"
  [exps env]
  (cond
    (last-exp? exps)
    (eval (first-exp exps) env)

    :else
    ; NOT SURE THIS IS VALID CLOJURE
    (eval (first-exp exps) env)
    (eval-sequence (rest-exps exps) env)))

(defn eval-assignment
  [exp env]
  "Handles assignments to variables.

  It calls eval to find the value to be assigned and transmits the variable
  and the resulting value to set-variable-value! to be installed in
  the designated environment."
  (set-variable-value! (assignment-variable exp)
                       (eval (assignment-value exp) env)
                       env)
  ’ok)

(defn
  eval-definition
  [exp env]
  (define-variable! (definition-variable exp)
                    (eval (definition-value exp) env)
                    env)
  ’ok)

;;
(defn self-evaluating? [exp]
  (cond (number? exp) true
        (string? exp) true
        :else false))

(defn variable? [exp] (symbol? exp))

(defn quoted? [exp] (tagged-list? exp ’quote))
(defn text-of-quotation [exp] (rest exp))
(defn tagged-list? [exp tag]
  (if (pair? exp)
  (eq? (car exp) tag)
  false))


(defn eval
  "Evaluate an expression"
  [eval exp env]
  (cond
    (self-evaluating? exp) exp)
    (variable? exp) (look-up-variable-value exp env)
    (assignment? exp) (eval-assignment exp env)
    (definition? exp) (eval-definition exp env)
    (if? exp) (eval-if expr env)
    (lambda? exp)
      (make-procedure (lambda-parameters exp)
                      (lambda-body exp)
                      env)
    (begin? exp)
      (eval-sequence (begin-actions exp) env)
    (application? exp)
      (apply (eval (operator exp) env)
             (list-of-values (operands exp) env))
    :else
      (error "Unknown expression typoe: EVAL" exp))

(defn apply
  "Apply"
  [procedure arguments]
  (cond
    (primitive-procedure? procedure)
    (apply-primitive-procedure procedure arguments)

    (compound-procedure? procedure)
    (eval-sequence)
      (procedure-body procedure)
      (extend-environment
        (procedure-parameters procedure)
        arguments
        (procedure-evnironment procedure))

    :else
      (error "unknown procedure type:apply procedure")))