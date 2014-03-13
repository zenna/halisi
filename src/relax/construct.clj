(ns ^{:doc "Construct"
      :author "Zenna Tavares"}
  relax.construct
  (:require [veneer.pattern.match :refer :all]
            [veneer.pattern.rule :refer :all])
  (:require [clozen.helpers :as clzn]
            [clozen.iterator :as itr]
            [clozen.zip :refer :all])
  (:require [clojure.walk :refer [postwalk-replace]]
            [clojure.zip :as zip]))

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
  "Source codes for some standard functions"
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
  "Lookup the source of a compound function"
  [f]
  (fn-codes f))

(defn compound?
  "Is this function a compound"
  [f]
  (if (lookup-compound f)
      true
      false))

;; FIXME
(defn defined-symbol?
  "Is the symbol defined?"
  [x]
  (or (primitive? x) (compound? x)))

(defn evaluated?
  "Is x fully evaluated?
   X is evaluated if it is not a list.
   If it is a list then "
  [x]
  (if (coll? x)
      (if (= (first x) 'quote)
          true
          (every? evaluated? (seq x)))
      (not (symbol? x))))
      
;; Rules
; For primitive function evaluation I need to know that the arguments
; are fully evaluated.
(use 'veneer.pattern.dsl)
(use 'clozen.debug)

(defn constrained-node-itr
  [lhs exp]
  (let [itr (itr/node-itr exp)]
    (if (coll? lhs)
        (itr/add-itr-constraint itr #(coll? (itr/realise %)))
        itr)))

(def primitive-apply-rule
  "This rule applies a primitive function"
  (rule '->
        (->CorePattern (match-fn x
                         ([f & args] :seq) {:f f :args args}
                         :else nil))
        (fn [{f :f args :args}]
          (apply (primitive f) args))
        (->ExprContext
          itr/node-itr
          (fn [{f :f args :args}]
            (and (primitive? f)
                 (evaluated? args))))
        nil))

; (defrule primitive-apply-rule
;   "Apply primitive functions"
;   (-> (?f & args) (apply ?f args) :when (and (primitive? (primitive ?f))
;                                              (evaluated? args))))


(def compound-f-sub-rule
  "Substitute in a compound function"
  (rule '->
        (->CorePattern (match-fn x
                         ([f & args] :seq) {:f f :args args}
                         :else nil))
        (fn [{f :f args :args}]
          `(~(lookup-compound f) ~@args))
        (->ExprContext
          itr/subtree-itr
          (fn [{f :f}]
            (compound? f)))
        nil))

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
  (->ExprContext
    itr/subtree-itr
    (fn [_ &]
      true))
  nil))

(def variable-sub-rule-nullary
  "Substitute in variables"
  (rule 
  '->
  (->CorePattern (match-fn x
                   (['fn [] body] :seq) {:body body}
                   :else nil))
  (fn [{body :body}]
    body)
  (->ExprContext
    itr/subtree-itr
    (fn [_ &]
      true))
  nil))


;; IF
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
  (->ExprContext
    itr/subtree-itr
    (fn [_ &]
      true))
  nil))
  
(declare check-if check-parents)

(defn check-parents [zip-tree]
  "Used in combination with check-parents."
  (if (nil? (zip/up zip-tree))
            true
            (check-if (zip/up zip-tree))))

(defn check-if [zip-tree]
  "Determine where a loc
   in a zip is in a confirmed branch"
  (loop [zip-tree zip-tree]
    (let [locs-list (base zip-tree)]
      (if
        (= 'if (first locs-list)) ;in if branch
        (cond
          (= 1 (zip-loc-pos zip-tree)) ; I'm the condition
          (check-parents zip-tree)

          (= 2 (zip-loc-pos zip-tree)) ; I'm the consequent
          (if (true? (second locs-list))
            (check-parents zip-tree)
            false)

          (= 3 (zip-loc-pos zip-tree)) ; I'm the alternaive
          (if (false? (second locs-list))
            (check-parents zip-tree)
            false)

          :else
          false)
        (check-parents zip-tree)))))

(defn -main []
  (do
    ; (def it (itr/add-itr-constraint itr/node-itr #(coll? (itr/realise %))))
    ; (itr/iterate-and-print a-exp it)
    
    (def a-exp '(if false (+ 3 (if (> -12 3) 0 (mean [1 2 3]))) 12))
    (def rules [compound-f-sub-rule variable-sub-rule-nullary variable-sub-rule  primitive-apply-rule if-rule ])
    (def named-rules (map #(assoc %1 :name %2) rules
      '[compound-f-sub-rule variable-sub-rule-nullary if-rule variable-sub-rule primitive-apply-rule]))
    (def transformer (partial eager-transformer named-rules))
    (rewrite a-exp transformer)
  ))

(comment
  (def exp '(fn [n f]
       (loop [n n res [] d [2 1] e {:a 12 :b alpha}]
        (if (zero? n) res
            (recur (dec n) (conj res (f)))))))

  (iterate-and-print-fn x #(-> % realise evaluated?)))