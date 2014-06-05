(ns ^{:doc "Construct"
      :author "Zenna Tavares"}
  sigma.construct
  (:require [veneer.pattern.match :refer :all]
            [veneer.pattern.rule :refer :all]
            [veneer.pattern.dsl-macros :refer [defrule]]
            [veneer.pattern.match-macros :refer [match-fn]]
            [clozen.helpers :as clzn]
            [clozen.iterator :as itr]
            [clozen.zip :refer :all]
            [clojure.walk :refer [postwalk-replace]]
            [clojure.zip :as zip]))

;; Deterministic semantics - rewrite rules for non-probabilstic sigma-rewrite
(def primitives-coll
  '[+ * - + / > < >= <= apply rand reduce count])

(def primitives (zipmap primitives-coll (map eval primitives-coll)))

(defn primitive
  "Get evaluable function from symbol"
  [symb]
  (if-let [f (primitives symb)]
    f
    (throw (Throwable. "Tried to get primitive from non-primitive"))))

(defn prim-arithmetic?
  "FIXME:SLOW"
  [symb]
  (or (clzn/in? (map primitives '[+ * - + /]) symb)))

(defn primitive-symbol?
  "Is the symbol a primitive?"
  [symb]
  (clzn/in? primitives-coll symb))

(defn primitive-fn?
  "Is the symbol a primitive?"
  [symb]
  (clzn/in? (vals primitives) symb))

(def global-env
  "Source codes for some standard functions"
  (atom
    {'repeat
    '(fn [n f]
       (loop [n n res []]
        (if (zero? n) res
            (recur (dec n) (conj res (f))))))
    'mean
    '(fn [coll]
       (/ (reduce + coll)
          (count coll)))}))

(defn update-ns
  "Add a value to the namespace"
  [name value]
  (println "Updating variable" name "to" value)
  (swap! global-env assoc name value))

(defn lookup-compound
  "Lookup the source of a compound function"
  [f]
  (@global-env f))

(defn compound?
  "Is this function a compound"
  [f]
  (if (lookup-compound f)
      true
      false))

(defn evaluated?
  "Is x fully evaluated?
   X is evaluated if it is not a list.
   If it is a list then "
  [x]
  (if (coll? x)
      (cond
        (= (first x) 'quote)
        true
        (primitive-fn? (first x))
        false
        :else
        (every? evaluated? (seq x)))
      (not (symbol? x))))

;; Rules ======================================================================
(defn constrained-node-itr
  [lhs exp]
  (let [itr (itr/node-itr exp)]
    (if (coll? lhs)
        (itr/add-itr-constraint itr #(coll? (itr/realise %)))
        itr)))

(defrule primitive-apply-rule
  "Apply primitive functions"
  (-> (?f & args) (apply ?f args) :when (and (primitive-fn? ?f)
                                             (every? evaluated? args))))

(defrule eval-primitives
  "Eval primitive functions"
  (-> x (primitive x) :when (primitive-symbol? x)))

(defrule compound-f-sub-rule
  "Substitute in a compound function"
  (-> (?f & args) `(~(lookup-compound ?f) ~@args) :when (compound? ?f)))

(defn mapx [f coll]
  (if (vector? coll)
      (mapv f coll)
      (map f coll)))

(defn body-replace
  "Substitutes arguments into the body of a function"
  ([body bindings] (body-replace body bindings #{}))
  ([body bindings rebound]
  (cond
    (and (symbol? body)
         (bindings body)
         (not (rebound body)))
    (bindings body)

    (and (coll? body) (= (first body) 'fn))
    (mapx #(body-replace % bindings
                          (clzn/merge-sets [rebound (second body)])) body)

    (coll? body)
    (mapx #(body-replace % bindings rebound) body)

    :else body)))

(defrule variable-sub-rule
  "A variable substitution rule"
  (-> ((fn [& args] body) & params) (body-replace body (zipmap args params))))

(defrule variable-sub-rule-nullary
  "A variable substitution rule"
  (-> ((fn [] body)) body))

(defrule let-to-fn-rule
  "Convert let to lambda"
  (-> ('let [& args] body)
      (let [bindings (partition 2 args)
            [var-name var-bind] (last bindings)
            rest-binds (vec (take (- (count args) 2) args))
            bound-fn `((~'fn [~var-name] ~body) ~var-bind)]
        (if (empty? rest-binds)
            bound-fn
            `(~'let ~rest-binds ~bound-fn)))
      :when (even? (count args))))

(def if-rule
  "Substitute in variables"
  (rule
  '->
  (->CorePattern
    (match-fn
      (['if true consequent alternative] :seq)
      {:branch consequent}
      (['if false consequent alternative] :seq)
      {:branch alternative}
      :else nil))
  (fn [{branch :branch}]
    branch)
  (->ExprContext
    itr/subtree-itr
    (fn [_ &]
      true))
  nil))

;; TODO - hacked associativity
(defn associative-fn?
  "Is this operation associative"
  [f]
  (or (= f '+)
      (= f (primitives '+))))

(defrule associativity-rule
  "Associativity"
  (-> (?f (?g y z) x) (list ?f x y z) :when (and (= ?f ?g)
                                              (associative-fn? ?f))))

(defrule define-rule!
  "Define Rule"
  (-> ('def name value) (do (update-ns name value) nil)))

(defrule defn-rule
  "Defn to defn"
  (-> ('defn name docs args body) `(def ~name (~'fn ~args ~body))))

(def std-rules
  [eval-primitives compound-f-sub-rule variable-sub-rule-nullary variable-sub-rule  primitive-apply-rule if-rule associativity-rule let-to-fn-rule define-rule! defn-rule])

(comment
  (use '[fipp.edn :refer (pprint) :rename {pprint fipp}])
  ;; Define some expressions
  (def exp '(if true (+ 3 (if (> -12 3) 0 (mean [1 2 3]))) 12))
  (def exp '(fn [n f]
       (loop [n n res [] d [2 1] e {:a 12 :b alpha}]
        (if (zero? n) res
            (recur (dec n) (conj res (f)))))))
  (def exp
    '(fn [y]
       ((fn [x]
          ((fn [x]
            (+ x y))
          (+ x 12)))
        (+ 10 y))))

  (def exp-demo-let-to-fn
    '(fn [x]
      (let [a x b 20 c 30]
       (+ a b c))))

  (def closure-demo
    '(def get-twenty (let [x 10]
      (fn []
        (+ x x)))))

  (rewrite closure-demo transformer)
)
