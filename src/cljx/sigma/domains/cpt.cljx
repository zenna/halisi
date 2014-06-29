(ns ^{:doc "Conditional Probability Table Domain"
      :author "Zenna Tavares"}
  sigma.domains.cpt
  (:require [veneer.pattern.dsl-macros :refer [defrule]]
            [clozen.helpers :as clzn]
            [clozen.debug :as dbg]
            [clojure.set :as s]
            [clojure.core.matrix :as m]))


;; TODO
;; - Extend to arbitrary arity, mixed rv/scalar arguments
;; - Move necessary libraries out to clozen
;; - Defrule for categorical distribution
;; - Add sanity check - Sum of probabilities is 1
;; - Categorical distribution
;; - Catch errors pass undefined.
;; - Pass in Rules

(defn split-with-pos
  "Split a vector, and get positions"
  [pred? coll]
  (subvec
    (reduce
     (fn [[i & _ :as accum] elem]
       (if (pred? elem)
           (-> accum (update-in [1] #(conj % elem))
                     (update-in [3] #(conj % i))
                     (update-in [0] inc))
           (-> accum (update-in [2] #(conj % elem))
                     (update-in [4] #(conj % i))
                     (update-in [0] inc))))
     [0 [][][][]]
     coll)
     1))


;; Matrix Abstractions ========================================================
(defn concat-rows
  "Concat the rows for two matrices"
  [m1 m2]
  ; TODO: Highly inefficient transpose just to concat rows
  (m/transpose (m/join-along 0 (m/transpose m1) (m/transpose m2))))

(defn normalised?
  "Does the probability column sum to one?"
  [cpt]
  (clzn/tolerant= 1.0
                  (clzn/sum (first (m/columns (:entries cpt))))))

(defn normalise
  "Normalise a cpt"
  [cpt]
  (let [[probs & values] (m/columns cpt)]
    (m/matrix
     (m/transpose
      (concat [(map #(/ % (clzn/sum probs)) probs)]
              values)))))

;; Cpt abstractions ===========================================================
(defrecord Cpt
  ^{:doc "A conditional probability table"}
  [var-names entries primitives])

(defn Cpt? [x]
  (instance? Cpt x))

(defn ->Cpt [a b c]
  "Some bug in lighttable/clojure is making instance? not work with ->
   constructed values"
  (Cpt. a b c))

;; ============
;; Constructors
(defn ->cpt-single-var
 ^{:doc
   "A conditional probability table for a finite set of named variables
    Stores all combinations of values and associated probabilities
    In contrast to a joint probability table, this represents a a random variable

    var-names stores variable names
    entries - matrix where column 0 represents a probability
              column 0, .., n -1 are values for each dependent variable
              column n is value for independent variable"}
  [var-name probs var-values]
  {:pre [(clzn/tolerant= (clzn/sum probs) 1.0)
         (clzn/count= var-values probs)]}
  (->Cpt [var-name] (m/transpose (m/matrix [probs var-values]))
                    {var-name (m/transpose (m/matrix [probs var-values]))}))

(defn cpt-uniform
  "Create a uniform conditional probability table"
  [var-name low-bound up-bound]
  (let [[a b c] [var-name low-bound up-bound]
        n (- up-bound low-bound)]
    (->cpt-single-var var-name (vec (repeat (inc n) (/ 1 (inc n))))
                               (range low-bound (inc up-bound)))))

(defn ->flip-int
  "Bernoulli"
  [var-name weight]
  (->cpt-single-var var-name [(- 1 weight) weight] [0 1]))

(defn ->flip
  "Bernoulli distribution over true/false"
  [var-name weight]
  (->cpt-single-var var-name [(- 1 weight) weight] [false true]))

;; ============
;; Abstractions

(defn row-probability
  [row]
  (first row))

(defn row-entries
  [row]
  (next row))

(defn independent-var-name
  "Variable name of the indepedent random variable"
  [cpt]
  (last (:var-names cpt)))

(defn var-name-values
  "Get values of a particular variable - entire column, may include duplicates"
  [cpt var-name]
  (m/get-column
   (:entries cpt)
   ; inc because first col is probability col
   (inc (clzn/first-index (:var-names cpt) var-name))))

;; =================
;; Functions on cpts

(defn merge-tables
  "Merge two tables by finding cart-product of values and multiplying probabilities"
  [table1 table2]
  (->Cpt
    (vec (concat (:var-names table1) (:var-names table2)))
    (m/matrix
      (for [table1-row (m/rows (:entries table1))
            table2-row (m/rows (:entries table2))
            :let [product-prob (* (row-probability table1-row)
                                  (row-probability table2-row))]]
        (concat [product-prob]
                (row-entries table1-row)
                (row-entries table2-row))))
    nil))

(defn filter-row-by-var-name
  "Return a new row, whose variable name satisfies pred?
   Can use this to restrict rows to particular columns by name"
  [pred? cpt row]
  (let [mask (concat [false] ; prepend false to remove probability element
                     (mapv pred? (:var-names cpt)))
        row-with-nils (mapv #(if %1 %2 nil) mask row)]
    (vec (remove nil? row-with-nils))))

(defn intersect-vec-set
  "Remove elements from a vector v if also in s.
   I.e. its like an ordered set intersection"
  [v s]
  (vec (remove nil? (map s v))))

(defn add-dependent-vars
  "First we find the all combinations of the primitive variables
   Now we need to add back the non-primitive (i.e. values which are functions
   of primitive rvs) to the conditional probability table

   We do this (TODO: Very inefficiently) by for each row in the argument, findng the
   matching row in the new joint"
  [primitive-joint cpts]
  (reduce
    (fn [final-cpt arg-cpt]
      ;; Which of its rows are not already in the final cpt
      (let [unseen-vars (s/difference (set (:var-names arg-cpt)) (set (:var-names final-cpt)))]
        (if (seq unseen-vars)
            (let [union-vars (s/intersection (set (:var-names arg-cpt)) (set (:var-names final-cpt)))

                  ; We need to maintain the ordering of columns for correct map-looku later
                  ; hence, convert from set to vector
                  union-vars (intersect-vec-set (:var-names final-cpt) union-vars)
                  unseen-vars (intersect-vec-set (:var-names arg-cpt) unseen-vars)
                  x (mapv (partial var-name-values arg-cpt) union-vars) ; AB
                  y (mapv (partial var-name-values arg-cpt) unseen-vars)

                  ;; FIXME: when we convert var-names to set we will loose the ordering. Which is relevant
                  z (zipmap (m/transpose x) (m/transpose y))

                  ; For every row in joint, find relevant columns, look up val in arg cpt
                  mo-rows (for [row (m/rows (:entries final-cpt))]
                            (z (filter-row-by-var-name (set union-vars) final-cpt row)))]

              ; Add the new rows and add the new variable names
              (-> final-cpt (update-in [:entries] #(concat-rows % mo-rows))
                  (update-in [:var-names] #(vec (concat % unseen-vars)))))
          final-cpt)))
      primitive-joint
      cpts))

(defn apply-binary-cpt
  "Apply a function to cpt1 and cpt2"
  [f new-name cpt1 cpt2]
  (let [all-primitives (apply merge (map :primitives [cpt1 cpt2]))
        primitives (mapv (fn [[name table]] (->Cpt [name] table nil)) (seq all-primitives))
        primitive-combos (reduce merge-tables (first (seq primitives)) (next (seq primitives)))
        primitive-combos (assoc primitive-combos :primitives all-primitives)

        ; Add dependent variables
        primitive-combos (add-dependent-vars primitive-combos [cpt1 cpt2])

        ; Apply f to inependent values
        independent-cols (mapv #(var-name-values primitive-combos (independent-var-name %)) [cpt1 cpt2])
        asrt (dbg/asrt #(apply clzn/count= %) independent-cols)
        x (apply (partial mapv f) independent-cols)]
    (-> primitive-combos (update-in [:entries] #(concat-rows % x))
                         (update-in [:var-names] #(conj % new-name)))))


(defn primitive-joint
  [cpts]
  (let [all-primitives (apply merge (map :primitives cpts))
        primitives (mapv (fn [[name table]] (->Cpt [name] table nil)) (seq all-primitives))
        joint (reduce merge-tables (first (seq primitives)) (next (seq primitives)))]
    (assoc joint :primitives all-primitives)))

(defn apply-cpt
  [name f & cpts]
  (let [[cpts non-cpts cpt-pos non-cpt-po] (split-with-pos Cpt? cpts)
        joint (primitive-joint cpts)

        ; Add dependent variables
        joint (add-dependent-vars joint cpts)

        ; Apply f to inependent values
        independent-cols (mapv #(var-name-values joint (independent-var-name %)) cpts)
        x (apply (partial mapv f) independent-cols)]
    (-> joint (update-in [:entries] #(concat-rows % x))
              (update-in [:var-names] #(conj % name)))))

(defn if-f
  [condition consequent alternative]
  (if condition consequent alternative))


;; =======
;; Queries

(defn unnormalised-cond-dist
  "Conditional expectation of rv given pred?
   Currently only supports hard constraints on random variable itself"
  [pred? table]
  (m/matrix (filter #(pred? (last %)) (m/rows table))))

(defn cond-dist
  "Conditional distribution of rv given pred?
   Currently only supports hard constraints on random variable itself"
  [cpt pred?]
  (update-in cpt [:entries] #(normalise (unnormalised-cond-dist pred? %))))

(defn expectation
  "Expectation of a cpt"
  [cpt]
  (let [prob-col (first (m/columns (:entries cpt)))
        val-col (last (m/columns (:entries cpt)))]
  (double (m/esum (mapv * prob-col val-col)))))

;; =====
;; Rules

(defrule uniform-int->
  "Discrete Uniform Contructor"
  (-> ('uniform-int var-name x y) (cpt-uniform var-name x y)))

(defrule apply-binary-cpt->
  "Apply a function to a pair of random variables"
  (-> (?f cpt1 cpt2) (apply-binary-cpt 'TODO ?f cpt1 cpt2) :when (and (Cpt? cpt1) (Cpt? cpt2)
                                                                      (fn? ?f))))
(defrule expectation->
  "Compute the expectation of a cpt"
  (-> ('expectation cpt) (expectation cpt) :when (Cpt? cpt)))

(defrule flip->
  "Bernoulli Distribution over the integers"
  (-> ('flip var-name weight) (->flip var-name weight)))

(defrule cpt-if->
  "If for cpt condition.
   FIXME: This only works when all arguments are CPTS, make more general"
  (-> ('if condition consequent alternative)
      (apply-cpt 'TODO if-f condition consequent alternative)
      :when (and (Cpt? condition)
                 (Cpt? consequent)
                 (Cpt? alternative))))

(def rules [cpt-if-> flip-> uniform-int-> apply-binary-cpt-> expectation->])
  (require  '[veneer.pattern.transformer :as transformer :refer [rewrite]]
            '[sigma.construct :refer [std-rules]])
(defn -main
  []
  (require  '[veneer.pattern.transformer :as transformer :refer [rewrite]]
            '[sigma.construct :refer [std-rules]])
  (let [rules (concat rules std-rules)
        eager-transformer (partial transformer/eager-transformer rules)]
    (rewrite '(if (flip 'breast-cancer 0.01) (flip 'c 0.8) (flip 'a 0.096)) eager-transformer)))

(comment
  [veneer.pattern.transformer :as transformer :refer [rewrite]]
            [sigma.construct :refer [std-rules]]

  (def rules (concat [apply-binary-cpt-rule expectation-rule uniform-int] std-rules))
  (def eager-transformer (partial transformer/eager-transformer rules))

  (defn -main []
    (sigma-rewrite '(expectation (+ (uniform-int 'y 0 4) (uniform-int 'x 0 4))) eager-transformer)))

  ;; (sigma-rewrite
  ;;  '(let [x (uniform-int 'x 0 3)
  ;;         y (uniform-int 'y 0 3)] (+ x y)) eager-transformer))
