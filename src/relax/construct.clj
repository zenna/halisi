(ns ^{:doc "Construct"
      :author "Zenna Tavares"}
  relax.construct
  (:require [veneer.pattern.match :refer :all]
            [veneer.pattern.rewrite :refer :all])
  (:require [clozen.helpers :as clzn]
            [clozen.iterator :as itr])
  (:require [clojure.walk :refer [postwalk-replace]]))

(def primitives-coll
  '[+ * - + / > < >= <= apply rand reduce count])

(def primitives (zipmap primitives-coll (map resolve primitives-coll)))

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
  {'repeat
  '(fn [n f]
     (loop [n n res []]
      (if (zero? n) res
          (recur (dec n) (conj res (f))))))
  'mean
  '(fn [coll]
     (/ (reduce + coll)
        (count coll)))})

(defn lookup-compound
  [f]
  (fn-codes f))

(defn compound?
  [f]
  (if (lookup-compound f)
      true
      false))

;; For primitive function evaluation I need to know that the arguments
;; are fully evaluated.
(def primitive-apply-rule
  "This rule applies a primitive function"
  (rule '->
        (->CorePattern (match-fn x
                         ([f & args] :seq) {:f f :args args}
                         :else nil))
        (fn [{f :f args :args}]
          (apply (primitive f) args))
        itr/subtree-leaves-first-itr
        seq?
        (fn [{f :f}]
          (primitive? f))))

(def compound-f-sub-rule
  "Substitute in a compound function"
  (rule '->
        (->CorePattern (match-fn x
                         ([f & args] :seq) {:f f :args args}
                         :else nil))
        (fn [{f :f args :args}]
          `(~(lookup-compound f) ~@args))
        itr/subtree-itr
        seq?
        (fn [{f :f}]
          (compound? f))))

(def variable-sub-rule
  "Substitute in variables"
  (rule 
  '->
  (->CorePattern (match-fn x
                   ([(['fn [& args] body] :seq) & params] :seq)
                     {:args args :body body :params params}
                   :else nil))
  (fn [{args :args body :body params :params}]
    (postwalk-replace (zipmap args params) body))
  itr/subtree-itr
  seq?))

(def if-rule
  "Substitute in variables"
  (rule 
  '->
  (->CorePattern
    (match-fn x
      (['if true consequent alternative] :seq)
      {:branch consequent}
      (['if false consequent alternative] :seq)
      {:branch alternative}
      :else nil))
  (fn [{branch :branch}]
    branch)
  itr/subtree-itr
  seq?))

(def variable-sub-rule-nullary
  "Substitute in variables"
  (rule 
  '->
  (->CorePattern (match-fn x
                   (['fn [] body] :seq) {:body body}
                   :else nil))
  (fn [{body :body}]
    body)
  itr/subtree-itr
  seq?))

(defn -main []
  (do
    (def a-exp '(+ 3 (if (> 4 3) 0 (mean [1 2 3]))))
    (def rules [compound-f-sub-rule variable-sub-rule-nullary if-rule variable-sub-rule primitive-apply-rule])
    (def transformer (partial eager-transformer rules))
    (rewrite a-exp transformer)
  ))