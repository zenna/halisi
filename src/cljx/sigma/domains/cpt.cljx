(ns ^{:doc "Conditional Probability Table Domain"
      :author "Zenna Tavares"}
  sigma.domains.cpt
  (:require
            [veneer.pattern.dsl-macros :refer [defrule]]
            [clozen.helpers :as clzn]
            [clozen.debug :as dbg]
            [clojure.set :as s]
            [clojure.core.matrix :as m]))


;; TODO

;; It is in a working condition, which is in cool.

;; - THe interpreter
;; The interpreter has become somehwat broken, I need to make it less broken and less fragile
;; Make unit tests
;; Fix let issue
;; fix def issue
;; fix rule for def without docstring
;; exception handling so we don't break the whole reduce-duplicate-rows
;; make it return values
;; traverse all elements of a list?
;; better errrors

;; the inference algorithms
;; we have an exponential algorithm here
;; - values should only maintain in their table ones they are directly dependent on

;; independence and the naming system

;; - make sure everything is well documented
;; get documentation in repl

;; - naming conventions
;; be consistent,

;; - bugs
;; there was a bug when i tried to normalise wit hmaxes Example

;; - recursion

;; - Sort out pre's

;; - Increase distribution familiee
;; Binomial
;; Geometric
;; Hyper-geometric


;; - Defrule for categorical distribution
;; - Catch errors pass undefined.

(defn if-f
  "If as a function"
  [condition consequent alternative]
  (if condition consequent alternative))

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
  "Normalise a cpt so that probability col sums to 1"
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

(defn ->categorical-cpt
  "Categorical Distribution"
  [var-name cats weights]
  {:pre [(clzn/count= cats weights)
         (clzn/tolerant= 1.0 (clzn/sum weights))]}
  (->cpt-single-var var-name weights cats))

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

(defn var-name-cpt
  [cpt var-name]
  (let [wobble (m/columns (:entries cpt))
        var-pos (inc (clzn/first-index (:var-names cpt) var-name))]
    (->cpt-single-var var-name (nth wobble 0) (nth wobble var-pos))))

(defn reduce-duplicate-rows
  "When entries have duplicate value rows,
   combine these by summing probabilities of duplicates"
  [entries]
  (println "Heya  ")
  (let [unique-vals-to-prob
        (reduce
          (fn [accum elem]
            (if (accum (next elem))
                (update-in accum [(next elem)] (partial + (first elem)))
                (assoc accum (next elem) (first elem))))
          {}
          (m/rows entries))]
    (m/matrix (mapv (fn [[values p]]
                                    (concat [p] values))
                              (seq unique-vals-to-prob)))))

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

(defn primitive-joint
  "Find the joint distributio by finding all combinations of primitive dists"
  [cpts]
  (let [all-primitives (apply merge (map :primitives cpts))
        primitives (mapv (fn [[name table]] (->Cpt [name] table nil)) (seq all-primitives))
        joint (reduce merge-tables (first (seq primitives)) (next (seq primitives)))]
    (assoc joint :primitives all-primitives)))

(defn apply-cpt
  "Apply a function to set of cpts"
  [name f args]
  (let [[cpts non-cpts cpt-pos non-cpt-pos] (clzn/split-with-pos Cpt? args)
        joint (add-dependent-vars (primitive-joint cpts) cpts)
        ; Apply f to inependent values
        independent-cols (mapv #(var-name-values joint (independent-var-name %)) cpts)
        constants (mapv #(repeat (count (first independent-cols)) %) non-cpts)
        all-args (clzn/unsplit independent-cols constants cpt-pos non-cpt-pos)
        x (apply (partial mapv f) all-args)]
    (-> joint (update-in [:entries] #(concat-rows % x))
              (update-in [:var-names] #(conj % name)))))

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

(defn collapse
  "Collapse a cpt to a var"
  [cpt var-name]
  (if-let [var-pos (clzn/first-index (:var-names cpt) var-name)]
    (#(->cpt-single-var var-name (nth % 0) (nth % (inc var-pos))) (m/columns (:entries cpt)))
    (throw (Exception. "tried to collapse to non-existent var-name"))))

(defn probability
  "What is the probability of taking this value
   TODO: Does a linear search each time. SUPERSLOW
   TODO: This could probably be subsumed by a proper expectation func"
  [cpt value]
  (let [columns (unnormalised-cond-dist #(= value %) (:entries cpt))]
    (if (seq columns)
        (m/esum (first (m/columns columns)))
          0.0)))

(defn refresh
  "Recalculate from primitives"
  [cpt]
  (update-in (add-dependent-vars (primitive-joint [cpt]) [cpt])
             [:entries]
             reduce-duplicate-rows))

(defn propagate
  [cpt restricted-prop]
  ; Find overlap between cpts primitives and variables in restricted-prop
  (let [primitives (s/intersection (set (keys (:primitives cpt)))
                                   (set (:var-names restricted-prop)))
        primitives (vec primitives)
        primitive-cpts (mapv #(:entries
                               (update-in (var-name-cpt restricted-prop %)
                                         [:entries]
                                         reduce-duplicate-rows))
                              primitives)]
    (refresh
     (update-in cpt [:primitives] #(merge % (zipmap primitives primitive-cpts))))))

(defn cpt-contains?
  "Does cpt1 contain cpt2 as a dependent variable"
  [cpt1 cpt2]
  (if ((set (:var-names cpt1)) (independent-var-name cpt2))
      true
      false))

(defn condition
  "Condition a cpt, such that prop-cpt is true"
  [cpt prop-cpt]
  (let [shared-vars (s/intersection (set (:var-names cpt)) (set (:var-names prop-cpt)))]
    (if (seq shared-vars)
        (let [restricted-prop (update-in prop-cpt [:entries] #(normalise (unnormalised-cond-dist true? %)))]
          (if (cpt-contains? prop-cpt cpt) ; If conditioned cpt contains other, we needn't propagate
              (update-in restricted-prop [:entries] reduce-duplicate-rows)
              (propagate cpt restricted-prop)))
        cpt)))

(defn expectation
  "Expectation of a cpt"
  [cpt]
  (let [prob-col (first (m/columns (:entries cpt)))
        val-col (last (m/columns (:entries cpt)))]
  (double (m/esum (mapv * prob-col val-col)))))

;; =====
;; Rules

(defrule apply-cpt->
  "Apply a function to a pair of random variables"
  (-> (?f & args) (apply-cpt 'TODO ?f args) :when (and (some Cpt? args) (fn? ?f))))

(defrule cpt-if->
  "If for cpt condition.
   FIXME: This only works when all arguments are CPTS, make more general"
  (-> ('if condition consequent alternative)
      (apply-cpt 'TODO if-f [condition consequent alternative])
      :when (and (Cpt? condition)
                 (Cpt? consequent)
                 (Cpt? alternative))))

;; Distribution Constructors
(defrule uniform-int->
  "Construct a uniform integer distribution
   (uniform-int 'x 0 10)"
  (-> ('uniform-int var-name x y) (cpt-uniform var-name x y)))

(defrule categorical->
  "Construct a categorical distribution
   (categorical 'x '[apple orange pair peach] [0.3 0.1 0.2 0.4])"
  (-> ('categorical var-name cats weights) (->categorical-cpt var-name cats weights)))

;; Queries
(defrule condition->
  "Condition a discrete

   (let [x (uniform-int 'x 0 10)
         y (uniform-int 'y 2 4)]
     (condition x (even? (* x y))))"
  (-> ('condition cpt prop) (condition cpt prop) :when (and (Cpt? cpt) (Cpt? prop))))

(defrule expectation->
  "Compute the expectation of a cpt"
  (-> ('expectation cpt) (expectation cpt) :when (Cpt? cpt)))

(defrule flip->
  "Bernoulli Distribution over the integers"
  (-> ('flip var-name weight) (->flip var-name weight)))

(def cpt-rules [cpt-if-> condition-> flip-> uniform-int-> apply-cpt-> expectation->])

;; ========
;; Examples

(comment

    (let [breast-cancer (flip 'breast 0.01)
      positive-mamogram (if breast-cancer (flip 'c 0.8) (flip 'a 0.096))]
  (condition breast-cancer positive-mamogram))


  (def a (cpt-uniform 'a 0 3))
  (def b (apply-cpt 'b + [a 3]))
  (def c (apply-cpt 'c - [a 2]))
  (def prop (apply-cpt 'prop > [b 4]))

  (condition c prop)

  (def x (cpt-uniform 'x 0 1))
  (def y (cpt-uniform 'y 0 1))
  (def c (apply-cpt 'c - [x 4]))
  (def sum (apply-cpt 'sum + [x y]))
  (def prop (apply-cpt 'prop >= [sum 1]))
  (condition c prop)


  (def cancer (->flip 'cancer 0.01))
  (def conseq (->flip 'coseq 0.8))
  (def alt (->flip 'alt 0.096))
  (def mamogram (apply-cpt 'mamo if-f [cancer conseq alt]))

  (condition cancer mamogram))
