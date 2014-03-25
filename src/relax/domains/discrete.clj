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
  "Create a discrete distribution supported on th
   [[0.25 0.25 0.25 0.25][1 2 3 4]]"
  [n probs]
  {:pre [(clzn/tolerant= (clzn/sum probs) 1.0)
         (clzn/count= n probs)]}
  [(vec probs) (vec n)])

(defn normalise
  "Normalise a discrete distribution"
  [[probs & rand-vars :as int-abo]]
  (assoc int-abo 0 (map #(/ % (clzn/sum rand-vars)) probs)))

(defn normalised?
  [[probs & rand-vars]]
  (clzn/tolerant= (clzn/sum probs) 1.0))

(defn discrete?
  "Is this object a discrete distribution
   FIXME: THIS IS WRONG, QUICK HACK"
  [x]
  (and (vector? x)
       (normalised? x)))

(defn uniform-discrete
  "Construct a uniform discrete distribution"
  [n]
  (discrete (range n) (vec (repeat n (/ 1 n)))))

;; Abstractions ===============================================================
(defn rows
  [int-abo]
  (clzn/transposev int-abo))

(comment
  (rows (uniform-discrete 4)))

(defn prob
  [line]
  (first line))

(defn indep-val
  [line]
  (last line))

(defn r-vals
  [line]
  (subvec line 1))

(defn line
  "construct a line"
  [p r-values]
  ; (println  "r" r-values)
  (concat [p] r-values))

(defn lines-to-int-abo
  [lines]
  (vec (clzn/transposev lines)))

(defn indep-var
  "Get the independent variable from a mofo"
  [int-abo]
  (last int-abo))

(comment
  (normalised? (uniform-discrete 10)))

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
      (line p (vec (concat (r-vals line-a) (r-vals line-b)
                           [(f (indep-val line-a) (indep-val line-b))]))))))

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