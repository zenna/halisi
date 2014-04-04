(ns ^{:doc "Finite Discrete Abstract Domain"
      :author "Zenna Tavares"}
  relax.domains.discrete
  (:require [relax.construct :refer :all])
  (:require [veneer.pattern.rule :refer :all]
            [veneer.pattern.match :refer :all]
            [veneer.pattern.dsl :refer [defrule]])
  (:require [clozen.helpers :as clzn])
  (:require [clojure.math.combinatorics :as combo]))

;; Constructors and Type testers ==============================================
(defn discrete
  "Factory for a finite discrete abstract object (fd-ao).
   These are represented as conditional probabbility tables (cpt)
   A discrete distribution is supported on the integers.
   Has (randomly) named variables
   Values for these variables and associated probabilities. 
   ['discrete 'rv-x [0.25 0.25 0.25 0.25] [1 2 3 4]]"
  [n probs]
  {:pre [(clzn/tolerant= (clzn/sum probs) 1.0)
         (clzn/count= n probs)]}
  ['discrete [(gensym 'rv)] (vec probs) (vec n)])

(defn discrete?
  "Is this object a discrete distribution"
  [obj]
  (and (vector? obj)
       (= 'discrete (first (obj)))))

(defn discrete-uniform
  "Construct a uniform discrete distribution"
  [n]
  (discrete (range n) (vec (repeat n (/ 1 n)))))

;; Abstractions ===============================================================
(defn rv-vals-probs
  "Get only dependent and independent values and probabilities as matrix"
  [abo]
  (subvec abo 2))

(defn rows
  "Get value data as a set of rows"
  [abo]
  (clzn/transposev (rv-vals-probs abo)))

(defn prob
  "What's the probability of a set of values of different variables?"
  [row]
  (first row))

(defn probs
  "What are all probabilities of cpt"
  [cpt]
  (cpt 2))

(defn indep-val
  "What's the independent variable of a row"
  [row]
  (last row))

(defn rv-vals
  "What are just the values, and not the probabilities"
  [row]
  (subvec row 3))

(defn row
  "construct a row"
  [p r-values]
  (concat [p] r-values))

(defn rows-to-cpt
  "Convert a collection of rows to a cpt."
  [rows]
  (vec (clzn/transposev rows)))

(defn indep-var
  "Get the independent variable from a cpt"
  [cpt]
  (last cpt))

(defn names "variable names" [cpt]
  (second cpt))

(defn name-to-column
  "Return values of a random variable, given its name"
  [cpt name]
  ((rv-vals cpt) (clzn/asrt #(>= % 0) (.indexOf (names cpt) name))))

;; Little Helpers =============================================================
(defn normalise
  "Normalise a discrete distribution"
  [[probs & rand-vars :as cpt]]
  (assoc cpt 0 (map #(/ % (clzn/sum rand-vars)) probs)))

(defn normalised?
  "Is this cpt normalised?"
  [cpt]
  (clzn/tolerant= (clzn/sum (probs cpt)) 1.0))

(comment
  (normalised? (discrete-uniform 10)))

;; Rule Machinery =============================================================
(defn apply-unary-f
  "Apply a unary function to a cpt"
  [f cpt]
  (conj cpt (mapv f (indep-var cpt))))

(defn unique-rvs
  "Find joint distribution.
   - For all combinations of values of variables
   - Find Joint Probability"
  [cpts]
  (letfn [(clunk [cpt msg]
            (let [unseen-vars (remove #(clzn/in? (map first msg) %)
                                      (names cpt))]
              (concat msg
                      (mapv #(vector % (name-to-column cpt %)) unseen-vars))))]
    (clzn/pass clunk [] cpts)))

(defn joint
  "Find joint distribution.
   - For all combinations of values of variables
   - Find Joint Probability"
  [cpts]
  (let [rvs (unique-rvs cpts)]
    (apply combo/cartesian-product (map second rvs))))

(defn apply-binary-f
  [f cpt-a cpt-b]
  (rows-to-cpt
    (for [row-a (rows cpt-a)
          row-b (rows cpt-b)
          :let [p (* (prob row-a) (prob row-b))]]
      (row p (vec (concat (rv-vals row-a) (rv-vals row-b)
                           [(f (indep-val row-a) (indep-val row-b))]))))))

(defn condition-discrete
  [abo pred?]


;; Rules ==================================================================
(defrule uniform-rule
  "Evaluates uniform to a discrete abo"
  (-> ('uniform n) (discrete-uniform n)))

(defrule arith-f-discrete-rule
  "Apply primitive arithmetic function to scalar value and abstraction of
   integer distribution"
  (-> (?f scalar abo) (apply-unary-f (partial ?f scalar) abo)
      :when (and (prim-arithmetic? ?f)
                 (number? scalar)
                 (discrete? abo))))

(defrule arith-f-binary-abo-rule
  "Apply primitive arithmetic function to two abstractions of
   integer distributions"
  (-> (?f abo-a abo-b) (apply-binary-f ?f abo-a abo-b)
      :when (and (prim-arithmetic? ?f)
                 (discrete? abo-b)
                 (discrete? abo-a))))

(defrule condition-rule
  "Condition a random variable"
  (-> ('conditon rv pred?) (condition-discrete rv pred?)
      :when (and (discrete? rv)
                 (fn? pred?))))

(comment
  (apply-binary-f + (discrete-uniform 2) (discrete-uniform 3)))

(defn -main []
  (require '[relax.interpret :refer :all])
  (def my-rules
    [eval-primitives compound-f-sub-rule variable-sub-rule-nullary variable-sub-rule if-rule associativity-rule let-to-fn-rule define-rule! defn-rule])
  (def unif-rules [uniform-rule arith-f-discrete-rule arith-f-binary-abo-rule])
  (def uni-transformer
    (partial eager-transformer (concat unif-rules my-rules)))
  (sigma-rewrite '(+ (uniform 2) (+ 1 (uniform 3))) uni-transformer))