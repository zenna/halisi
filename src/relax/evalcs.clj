(ns ^{:doc "A concrete and symbolic metacircular evaluator.
            
            This is a evaluator of lisp expressions as in SICP.
            Primary difference is that expressions are evaluated
            symbolically and abstractly.
            Input variables are symbolic values, and any function on them 
            shall yield a symbolic value.
            If symbolic values are found in the conditions of conditional
            functions such as if, then both branches must be explored (since
            the concrete value is unknown)."
      :author "Zenna Tavares"}
  relax.evalcs
  (:use relax.env)
  (:use relax.symbolic)
  (:use relax.common)
  (:use relax.conditionalvalue)
  (:use relax.multivalue)
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

;; Notes
; Are we doing symbolic execution or conversion of a logical expression to
; DNF?
; In normal symbolic execution we are typically looking for paths which lead to some output, perhaps an error.  In particular we are looking for constraints on the path
; Then, using some constraint solver we can determine whether these constraint are consistent, and hence, determine whether or not we can enounter this error on any permissible input.
; I analagously are looking for constraints which lead to true
; THe confusion stems from what precisely is a path in a functional program
; What if I don't explictly have true or false values.

; Something like
; (and
;   (or a b)
;   (or c d))
; There is no true or false explicirly, but (or a b), is a binary funtion for tru eand false and so is AND.
; (if (or a b)
;     5
;     6)
; The output is: 5|(a or b), which is 5|a or 5|b or 6|(not a and not b)
; SO what's special about if?
; It's not clear to me why if is special, on the one hand it is special because I need to define it to not explore both branches of a program.
; IS there a problem with evaluating both branches of an expression.
; Well efficiency, if only one value is going to be returned then I should only return that
; Side effects, if a branch has side effects which I do not wnat to be evaluated then that could be problmenatic
; Recursion.  It could lead to non termination.

; However if the entire interpretation is lazy, thenif need not be special
; in normal evaluation, but it is still special in symbolic evaluation.
; If should return a conditional value
; (if (> a b) 5 6)
; It's not clear how lazy evauation would work.  But for my intents
; Assume that
; (if (or (> x 2) false)
;     (> x 3)
;     true)

; ; Proposals:
; ; 1. if does produce a conditional value
; ; 2. (> x 3)|(> x 2) (> x 3)|false true|[(< x 2)^ (not false)]

; (and
;   (if (or (> x 2) false)
;     (> x 3)
;     true)
;   (or (y > 3) (y < 20)))

; TODO
; 1. I need a (defn handle-and) which if given symbolic input will do something interesting
; 1. I need a (defn handle-or) which if given symbolic input will do something interesting

; Interplay between conditional values and not
; 
; Subsumption
; Joins
; REMOVE FALSES
; COVERT NOT TRUES INTO FALSES AND VICE VERSA
; 

; (defn handle-and
;   "")




; or're doing symbolic execution, but conditioning on the output being true

; (if (or (> x 3))
;     10)

; THen it's a matter of turning conditional values into joint ones
; e.g. (> x 2) | true -> (> x 2)

; What actually has to change.
; 1. (and (or (and b c)))

(declare evalcs)

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

; necessary? our predicates should be pure
(defn assignment? [exp] (tagged-list? exp 'set!))
(defn assignment-variable [exp] (nth exp 1))
(defn assignment-value [exp] (nth exp 2))

(defn eval-assignment
  [exp env]
  "Handles assignments to variables.

  It calls eval to find the value to be assigned and transmits the variable
  and the resulting value to set-variable-value! to be installed in
  the designated environment."
  (set-variable-value! (assignment-variable exp)
                       (evalcs (assignment-value exp) env)
                       env)
  'ok)

; Lambda
(defn lambda? [exp] (tagged-list? exp 'lambda))
(defn lambda-parameters [exp] (nth exp 1))
(defn lambda-body [exp] (rest (rest exp)))
(defn make-lambda [parameters body]
  (list 'lambda (list parameters body)))

; Definitions
(defn definition? [exp] (tagged-list? exp 'define))
(defn definition-variable [exp]
  (if (symbol? (nth exp 1))
      (nth exp 1)
      (first (nth exp 1))))
(defn definition-value [exp]
  (if (symbol? (nth exp 1))
      (nth exp 2)
      (make-lambda (rest (nth exp 1))
                   (rest (rest exp)))))
(defn
  eval-definition
  [exp env]
  (define-variable! (definition-variable exp)
                    (evalcs (definition-value exp) env)
                    env)
  'ok)
  
(defn self-evaluating?
  [exp]
  (or (number? exp)
      (string? exp)
      (true? exp)
      (false? exp)))

(defn variable? [exp] (symbol? exp))
(defn quoted? [exp] (tagged-list? exp 'quote))
(defn text-of-quotation [exp] (rest exp))

;; Conditionals
(defn if? [exp] (tagged-list? exp 'if))
(defn if-predicate [exp] (nth exp 1))
(defn if-consequent [exp] (first (rest (rest exp))))
(defn if-alternative [exp]
  (if (not (empty? (rest (rest (rest exp)))))
      (first (rest (rest (rest exp))))
      'false))

(defn eval-if-concrete
  "Evaluate condition in the given environment.
   If cond is true, eval consequent, otherwise it eval the alternative"
  [exp eval-cond env]
  ; (println "EVAL-IF-CONCRETE" (evalcs (if-predicate exp) env) "HMM" env "\n")
  (if (true? eval-cond)
      (evalcs (if-consequent exp) env)
      (evalcs (if-alternative exp) env)))

(defn eval-if-symbolic
  "Evaluate an expression symbolically
   I want to find path constraints that lead to true"
  [exp eval-cond eval-cond-compl env]
  ; (println "EVAL-IF-SYMBOLIC" exp "Eval-Cond" eval-cond eval-cond-compl "\n")
  (let [constructor-args   ; (println "ENV" @env)\
    (pass
      (fn [[eval-cond consq-alt] cond-pairs]
        (if (feasible? eval-cond env)
            (let [evald-branch (evalcs (consq-alt exp) env)]
              (if (conditional-value? evald-branch)
                  (vec (concat cond-pairs
                    (interleave (all-values evald-branch)
                                (map #(merge-conditions % [eval-cond])
                                       (all-conditions evald-branch)))))
                  (conj cond-pairs evald-branch [eval-cond])))
            cond-pairs))
        []
        [[eval-cond if-consequent] [eval-cond-compl if-alternative]])]
    ; (println "unconditionified outpit os" constructor-args)
    (apply make-conditional-value constructor-args)))

    ; (make-merge-multivalue
    ;   (if (feasible? eval-cond env)
    ;       (multify add-condition (evalcs (if-consequent exp) env)
    ;                               [eval-cond])
    ;       'inconsistent)
    ;   (if (feasible? eval-cond-compl env)
    ;       (multify add-condition (evalcs (if-alternative exp) env)
    ;                               [eval-cond-compl])
    ;       'inconsistent)))

(defn eval-if-conditional
  "blag"
  [exp eval-cond-concrete env]
  ; (println "EVAL-IF-CONDITIONED" exp "HMM" eval-cond-concrete "\n")
    ; If it's a conditional value
    ; (if (if (> x1 3)
    ;         true
    ;         false) 
    ;     (if (< y 4) 'a 'b)
    ;     true)

    ; true | (> x1 3)
    ; false | (<= x1 3)

    ; a | (< y 1)
    ; b | (>= y 10)

    ; a | (< y 1) (> x1 3)
    ; b | ()
  ;so for each branch we check for consistency and concatenate
  (cond 
    (true? eval-cond-concrete)
    (evalcs (if-consequent exp) env)

    (false? eval-cond-concrete)
    (evalcs (if-alternative exp) env)

    :else
    (error "Condition should be true or false, not" eval-cond-concrete)))


; (defmacro multify-if
;   [pred a b]
;   `(let [eval-cond# ~pred]
;      (if (multivalue? eval-cond#)
;          true
;          (if eval-cond#
;               ~a
;               ~b))))

; NOTEST
(defn eval-if
  "Evaluate a predicate part of an if expression in the given environment.
   There are three cases, depending on whether the value is concrete, symbolic
   or a mutivalue"
  [exp env]
  (let [eval-cond (evalcs (if-predicate exp) env)]
    (multify
      (fn [eval-cond]
        ; (println "evalcond is" eval-cond "\n")
        (cond
          (symbolic? eval-cond)
          (eval-if-symbolic exp eval-cond (evalcs (negate (if-predicate exp)) env) env)

          (conditional-value? eval-cond)
          (handle-conditional eval-if-conditional exp eval-cond env)

          :else
          (eval-if-concrete exp eval-cond env)))

      eval-cond)))

(defn disjun?
  [exp]
  (tagged-list? exp 'disjun))

(defn disjun-operands
  [disjun]
  (second disjun))

(defn make-disjun
  [terms]
  {:pre [(set? terms)]}
  `(~'disjun ~terms))

(defn make-conjun
  [terms]
  `(~'conjun ~terms))

(defn conjun?
  [exp]
  (tagged-list? exp 'conjun))

(defn add-conjun-operands
  [conjun operands]
  (reduce conj conjun operands))

(defn conjun-operands
  [conjun]
  (second conjun))

(defn eval-disjoin
  [args]
  (make-disjun
    (loop [vals args disjun-terms #{}]
      ; (println "or operands" vals)
      (cond
        (empty? (first vals))
        disjun-terms

        (true? (first vals))
        (recur (rest vals) disjun-terms)

        (false? (first vals))
        #{false}

        (conjun? (first vals))
        (recur (rest vals) (conj disjun-terms (first vals)))

        (symbolic? (first vals))
        (recur (rest vals)
               (conj disjun-terms (first vals)))

        (disjun? (first vals))
        (recur (rest vals)
               (reduce conj disjun-terms (disjun-operands (first vals))))

        ; (conditional-value? (first vals))
        ; (recur (rest vals)
        ;        (concat (terms (first val)) disjun-terms))

        :else
        (error "unknown argument to or" val)))))

(defn or? [exp] (tagged-list? exp 'or))

(defn eval-or
  "or returns a disjunction, which is a set of values
   1. eval all the operands
   2. "
  [exp env]
  (eval-disjoin (list-of-values (operands exp) env)))

(declare eval-conjoin)

(defn handle-combos
  [[conjun-terms cart-prod]]
  "CASES: could be true/false
   Could be just a conkunction
   Could be empty
   -- {{}} if everything was true
   -- if there were no args
   "
   ; (println "cart-prod" cart-prod)
  (cond
    (false? cart-prod) false

    ; No disjunction terms
    (and (empty? conjun-terms) (empty? cart-prod))
    true

    (empty? cart-prod)
    (make-conjun conjun-terms)

    ; not empty? then I need to find the cart product
    :else
    (let [product (apply combo/cartesian-product cart-prod)
          ; pvar (println "cartesian product" product)
          disjun-terms
          (map (comp eval-conjoin #(concat % conjun-terms))
               product)]
      (eval-disjoin disjun-terms))))

(defn eval-conjoin
  [args]
  "if I see a disjunction then I'll make a new set call eval-conjoin on.
   What does AND return
   Well in concrete case it should return true or false
   If its arguments are ors then it should return a disjunction
   If its arguments are not ors then it should return a disjunction of one
   or a just a conjunction
   If we make it return a disjunction of one, how will we ever recognise a conjunction
   Let's say we make it return a conjunction for simplicity then
   ()

   first prim terms
   find or terms
   "
  ; (println "conjoin args" args "\n")
  (handle-combos
    (loop [vals args conjun-terms #{} cart-prod #{}]
      (cond
        (empty? (first vals))
        [conjun-terms cart-prod]

        (true? (first vals))
        (recur (rest vals) conjun-terms cart-prod)

        (false? (first vals))
        [false #{}] 

        (conjun? (first vals))
        (recur (rest vals)
               (reduce conj conjun-terms (conjun-operands (first vals)))
               cart-prod)

        (symbolic? (first vals))
        (recur (rest vals)
               (conj conjun-terms (first vals)) cart-prod)

        (disjun? (first vals))
        (recur (rest vals)
               conjun-terms
               (conj cart-prod (disjun-operands (first vals))))

        ; (conditional-value? (first vals))
        ; (recur (rest vals)
        ;        (concat (terms (first vals)) disjun-terms))

        :else
        (error "unknown argument to AND" val)))))

(defn and? [exp] (tagged-list? exp 'and))

(defn eval-and
  "or returns a disjunction, which is a set of values
   1. eval all the operands
   2. "
  [exp env]
  (eval-conjoin (list-of-values (operands exp) env)))

;; Procedures
(defn make-procedure [parameters body env]
  (list 'procedure parameters body env))
(defn compound-procedure? [p]
  (tagged-list? p 'procedure))
(defn procedure-parameters [proc] (nth proc 1))
(defn procedure-body [proc] (nth proc 2))
(defn procedure-environment [proc] (nth proc 3))

(defn primitive-procedure?
  [proc]
  (tagged-list? proc 'primitive))
(defn primitive-implementation
  [proc]
  (nth proc 1))

(def primitive-prodecures
  (list (list '+ +)
        (list '- -)
        (list '* *)
        (list '/ /)
        (list '= =)
        (list '> >)
        (list '>= >=)
        (list '< <)
        (list '<= <=)))

(def primitive-procedure-names
  (map first primitive-prodecures))

(def primitive-procedure-objects
  (map #(list 'primitive (nth % 1)) primitive-prodecures))

(defn apply-primitive-procedure
  [proc args]
  ; (println "apply primitive" proc args)
  ; (println "proc is" proc "args are " args)
  (multify-apply (primitive-implementation proc) args))

(defn apply-primitive-procedure-hybrid
  [proc args]
  ; (println "proc is" proc "args are " args)
  ; (println "applyung" proc (map concrete-part args))
  (make-hybrid
    (apply (primitive-implementation proc) (map concrete-part args))
    (list proc args)))

(defn apply-primitive-procedure-symbolic
  [op args]
  ; (println "proc is" proc "args are " args)
  ; (println "applyung" proc (map concrete-part args))
  (make-symbolic
    (cons op args)))

(defn applycs
  "Apply"
  [procedure arguments exp]
  (cond
    (primitive-procedure? procedure)
    (cond 
      (some symbolic? arguments)
      (apply-primitive-procedure-symbolic (operator exp) arguments)

      :else
      (apply-primitive-procedure procedure arguments))

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
  ; (println "EXP IS" exp)
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

    (and? exp) (eval-and exp env)
    (or? exp) (eval-or exp env)

    (application? exp)
      (applycs (evalcs (operator exp) env)
               (list-of-values (operands exp) env)
               exp)
    :else
      (error "Unknown expression type: EVAL" exp)))

(defn setup-environment
  []
  (let [initial-env  (extend-environment primitive-procedure-names
                                         primitive-procedure-objects
                                         the-empty-environment)]
    (define-variable! 'true true initial-env)
    (define-variable! 'false false initial-env)
    initial-env))

(def the-global-environment (setup-environment))
(def the-pure-environment (setup-environment))


; A conditioned value only really makes sense as a multivalue
; It's a value which can assume many different values conditioned on some conjunction of constraints
; it is probably better named a conditional value
; x | A1 ^ A2^ A3

; An if statement where the condition is symbolic returns a conditional value

; (defn -main []
;   (define-variable! 'x (make-multivalue 1 2 3) the-global-environment)
;   (define-symbolic! 'x the-global-environment)
;   (evalcs '(if (> x 2) 1 3) the-global-environment))