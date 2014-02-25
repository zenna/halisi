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

(comment
  (def a-pattern-agent (->PatternAgent [primitive-f self-eval]))
  (def exp '(+ 1 2 (* 3 0)))
  (rewrite-eval a-pattern-agent exp))