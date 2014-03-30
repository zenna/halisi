(ns ^{:doc "Discrete Domain"
      :author "Zenna Tavares"}
  relax.domains.discrete
  (:require [relax.construct :refer :all])
  (:require [veneer.pattern.rule :refer :all]
            [veneer.pattern.match :refer :all]
            [veneer.pattern.dsl :refer [defrule]])
  (:require [clozen.helpers :as clzn]))

;; Constructors and Type testers ==============================================
(defn discrete
  "Factory
   A discrete distribution is supported on the integers.
   Has (randomly) named variables
   Values for these variables and associated probabilities. 
   [[0.25 0.25 0.25 0.25][1 2 3 4]]"
  [n probs]
  {:pre [(clzn/tolerant= (clzn/sum probs) 1.0)
         (clzn/count= n probs)]}
  ['discrete (gensym 'rv) (vec probs) (vec n)])

(defn discrete?
  "Is this object a discrete distribution"
  [obj]
  (and (vector? obj)
       (= 'discrete (first (obj)))))

(defn uniform-discrete
  "Construct a uniform discrete distribution"
  [n]
  (discrete (range n) (vec (repeat n (/ 1 n)))))

;; Abstractions ===============================================================
(defn rv-vals-probs
  "Get only dependent and independent values and associated probabilities as matrix"
  [abo]
  (subvec abo 2))

(defn rows
  "Get value data as a set of lines"
  [abo]
  (clzn/transposev (rv-vals-probs abo)))

(defn prob
  "What's the probability of a set of values of different variables?"
  [line]
  (first line))

(defn indep-val
  "What's the independent variable of a line"
  [line]
  (last line))

(defn rv-vals
  "What are just the values, and not the probabilities"
  [line]
  (subvec line 1))

(defn line
  "construct a line"
  [p r-values]
  (concat [p] r-values))

(defn lines-to-int-abo
  [lines]
  (vec (clzn/transposev lines)))

(defn indep-var
  "Get the independent variable from a mofo"
  [int-abo]
  (last int-abo))

;; Little Helpers =============================================================
(defn normalise
  "Normalise a discrete distribution"
  [[probs & rand-vars :as int-abo]]
  (assoc int-abo 0 (map #(/ % (clzn/sum rand-vars)) probs)))

(defn normalised?
  [abo]
  (clzn/tolerant= (clzn/sum (probs abo)) 1.0))

(comment
  (normalised? (uniform-discrete 10)))

;; Rule Machinery =============================================================
(defn apply-f
  "Apply an arithmetic function of a scalar and an abstract object"
  [f int-abo]
  (conj int-abo (mapv f (indep-var int-abo))))

(defn binary-f-abo
  [f abo-a abo-b]
  (lines-to-int-abo
    (for [line-a (rows abo-a)
          line-b (rows abo-b)
          :let [p (* (prob line-a) (prob line-b))]]
      (line p (vec (concat (rv-vals line-a) (rv-vals line-b)
                           [(f (indep-val line-a) (indep-val line-b))]))))))

(defn condition-discrete
  [abo pred?]


;; Rules ==================================================================
(defrule uniform-rule
  "Evaluates uniform to a discrete abo"
  (-> ('uniform n) (uniform-discrete n)))

(defrule arith-f-discrete-rule
  "Apply primitive arithmetic function to scalar value and abstraction of
   integer distribution"
  (-> (?f scalar abo) (apply-f (partial ?f scalar) abo)
      :when (and (prim-arithmetic? ?f)
                 (number? scalar)
                 (discrete? abo))))

(defrule arith-f-binary-abo-rule
  "Apply primitive arithmetic function to two abstractions of
   integer distributions"
  (-> (?f abo-a abo-b) (binary-f-abo ?f abo-a abo-b)
      :when (and (prim-arithmetic? ?f)
                 (discrete? abo-b)
                 (discrete? abo-a))))

(defrule condition-rule
  "Condition a random variable"
  (-> ('conditon rv pred?) (condition-discrete rv pred?)
      :when (and (discrete? rv)
                 (fn? pred?))))

(comment
  (binary-f-abo + (uniform-discrete 2) (uniform-discrete 3)))

(defn -main []
  (require '[relax.interpret :refer :all])
  (def my-rules
    [eval-primitives compound-f-sub-rule variable-sub-rule-nullary variable-sub-rule if-rule associativity-rule let-to-fn-rule define-rule! defn-rule])
  (def unif-rules [uniform-rule arith-f-discrete-rule arith-f-binary-abo-rule])
  (def uni-transformer
    (partial eager-transformer (concat unif-rules my-rules)))
  (sigma-rewrite '(+ (uniform 2) (+ 1 (uniform 3))) uni-transformer))