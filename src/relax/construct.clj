(ns ^{:doc "Construct"
      :author "Zenna Tavares"}
  relax.construct)

; Strategy we nake construct a function
; We overload the random primitives such that they return special objects
; We overload the defines so that we get the source code

(def env (atom {}))

(defmacro aidef [def-name & body]
  "aidef is just def except that it stores the code of the define"
  (do
  	(swap! env #(merge % {def-name body}))
  	`(def ~def-name ~@body)))

(defprotocol Agent
  "Protocol for evaluation agent"
  (next-agent [agent old-exp new-exp]
    "After every rewrite this creates a 'new' agent.
    This is just a functional way to allow the agent to
    have memory, which may be important.")
  (rewrite [agent exp] 
    "The agent decides when it should stop rewriting.
     Returns nil when no more rewrites to do"))

(defrecord PatternAgent
  [rules])

(defrule primitive-f
  "evaluate primitive functions"
  f x1 .. xn -> (f x1 .. x2) where (and (function? f)
                                        (primitive? f)
                                        (evaluated? xi)))

(defrule self-eval
  "Evaluate a symbol in its environment"
  var:x -> (env x))

;; An implementation of an Agent

(defn unevaluated? [exp]
  (not (coll? exp)))

(extend-protocol Agent
  PatternAgent
  (next-agent [ag old-exp new-exp]
    ag)
  (rewrite [ag exp]
    "Apply pattern-rewrite rules until no more to apply"
    (if (unevaluated? exp)
        (apply-rules exp (. rules agent))
        nil)))

(defn rewrite-eval
  "Evaluate expression exp in environment env.
   Evaluation = sequence of rewrites performed by agent.
   Stops when agent says so."
  [agent exp]
  (loop [exp exp]
    (if-let [new-exp (rewrite agent exp)]
      (recur new-exp (next-agent exp new-exp))
      exp)))

(def primitives {'+ + '* * '- +})

(defn primitive
  "Get evaluable function from symbol"
  [symb]
  (if-let [f (primitives symb)]
    f
    (eval symb)))

(defn primitive?
  "Is the symbol a primitive?"
  [symb]
  (clzn/nil-to-false (primitives symb)))

(def fn-codes
  {repeat
  '(fn [n f]
     (loop [n n res []]
      (if (zero? n) res
          (recur (dec n) (conj res (f))))))})

(defn lookup-compound
  [f]
  (fn-codes f))

(defn compound?
  [f]
  (if (lookup-compound f)
      true
      false))

(comment
  (def a-rule
    (rule '->
          `(~'square ~(variable 'x)) ; lhs
          (fn [{x (variable 'x)}]           ; rhs
            `(~'* ~x ~x))
          (fn [_]           ; condition
            true)))

  (def mul-rule
    (rule '->
          `(~'* ~(variable 'x) ~(variable 'y)) ; lhs
          (fn [{x (variable 'x) y (variable 'y)}]           ; rhs
            (* x y))
          (fn [{x (variable 'x) y (variable 'y)}]           ; condition
            (and (number? x) (number? y)))))

  ;; For primitive function evaluation I need to know that the arguments
  ;; are fully evaluated.
  ;; We can put that condition in the rule itself or not
  ;; It seems like we'll be violating a separation principle if we put in the rule
  ;; In the metacircular evaluator, we use order to avoid this problem.
  ;; We could imagine a 
  (def primitive-apply-rule
    "This rule applies a primitive function"
    (rule '->
          `(~(variable 'f) ~(variable 'x) ~(variable 'y))
          (fn [{f (variable 'f) x (variable 'x) y (variable 'y)}]
            ((primitive f) x y))
          (fn [{f (variable 'f) x (variable 'x) y (variable 'y)}]
            (primitive? f))))

  (def compound-f-sub-rule
    "Substitute in a compound function"
    (rule '->
          (~(variable 'f) ~(variable 'x) ~(variable 'y))
          (fn [{f (variable 'f) x (variable 'x) y (variable 'y)}]
            `(~(lookup-compound f) x y))
          (fn [{f (variable 'f) x (variable 'x) y (variable 'y)}]
            (compound? f))))

  (def variable-substitution
    "Substitute in variables"
    (rule '->
          ()

  ;; Options we can say, once we find pattern, replace all children using postwalk
  ;; Or something
  ;; Or (rewrite body eager-transfomer [(rule -> xs ys)])
  ;; Or go from the bottom up, whenver I see a symbol, check what it's bound to
  (-> ((fn [?xs] ?body) ?ys) 

  (def a-exp '(+ (square (square 3)) 2))
  (def b-exp '(* 2 9))
  (def transformer (partial eager-transformer [a-rule primitive-apply-rule]))
  (rewrite a-exp transformer)
  (def result (rewrite a-exp transformer)))

(comment
  (def a-pattern-agent (->PatternAgent [primitive-f self-eval]))
  (def exp '(+ 1 2 (* 3 0)))
  (rewrite-eval a-pattern-agent exp))