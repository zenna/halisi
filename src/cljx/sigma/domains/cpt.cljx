(ns ^{:doc "Conditional Probability Table Domain"
      :author "Zenna Tavares"}
  sigma.domains.conditional-table
  (:require [veneer.pattern.dsl-macros :refer [defrule]]
            [clozen.helpers :as clzn]
            [clozen.debug :as dbg]
            [clojure.set :as s]))

;; ========================= INSTA REPL
(require '[sigma.construct :refer :all]
         '[veneer.pattern.rule :refer [context-itr]]
         '[veneer.pattern.transformer :as transformer :refer [rewrite]]
         '[clojure.core.matrix :as m]
         '[clojure.set :as s])
;=====================================

(def sigma-rewrite rewrite)

;; TODO
;; - "Fill in" Dependent values
;; - Extend to arbitrary arity, mixed rv/scalar arguments
;; - Move necessary libraries out to clozen
;; - Defrule for categorical distribution
;; - Add sanity check - Sum of probabilities is 1

;; Helpers - Some To Move to Clozen Helprs ====================================
(defn first-index
  "Finds index of value in coll, nil if not found"
  [coll value]
  (let [i (.indexOf coll value)]
    (if (= -1 i)
        nil
        i)))

;; Matrix Abstractions ========================================================
(defn concat-rows
  "Concat the rows for two matrices"
  [entries1 entries2]
  ; TODO: Highly inefficient transpose just to concat rows
  (m/transpose (m/join-along 0 (m/transpose entries1) (m/transpose entries2))))

;; Cpt abstractions ===========================================================
(defn row-probability
  [row]
  (first row))

(defn row-entries
  [row]
  (next row))

(defn independent-var-name
  [cpt]
  (last (:var-names cpt)))

(defn var-name-values
  "Get values of a particular variable - entire column, may include duplicates"
  [cpt var-name]
  (m/get-column
   (:entries cpt)
   ; inc because first col is probability col
   (inc (first-index (:var-names cpt) var-name))))

(defrecord Cpt
  ^{:doc "A conditional probability table"}
  [var-names entries primitives])

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

(defn Cpt? [x]
  (instance? Cpt x))

(defn cpt-uniform
  "Create a uniform conditional probability table"
  [var-name low-bound up-bound]
  (let [n (- up-bound low-bound)]
    (->cpt-single-var var-name (vec (repeat (inc n) (/ 1 (inc n))))
                               (range low-bound (inc up-bound)))))

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
  [primitive-joint cpt1 cpt2]
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
      [cpt1 cpt2]))


(defn apply-binary-cpt
  [f cpt1 cpt2 new-name]
  (let [all-primitives (apply merge (map :primitives [cpt1 cpt2]))
        primitives (mapv (fn [[name table]] (->Cpt [name] table nil)) (seq all-primitives))
        primitive-combos (reduce merge-tables (first (seq primitives)) (next (seq primitives)))
        primitive-combos (assoc primitive-combos :primitives all-primitives)

        ; Add dependent variables
        primitive-combos (add-dependent-vars primitive-combos cpt1 cpt2)

        ; Apply f to inependent values
        independent-cols (mapv #(var-name-values primitive-combos (independent-var-name %)) [cpt1 cpt2])
        asrt (dbg/asrt #(apply clzn/count= %) independent-cols)
        x (apply (partial mapv f) independent-cols)]
    (-> primitive-combos (update-in [:entries] #(concat-rows % x))
                         (update-in [:var-names] #(conj % new-name)))))

(defn sum-to-one?
  "Probability column should sum to one"
  [cpt]
  (clzn/tolerant= 1.0
                  (clzn/sum (first (m/columns (:entries cpt))))))

(defrule apply-binary-cpt-rule
  "Apply a function to a pair of random variables"
  (-> (?f cpt1 cpt2) (apply-binary-cpt ?f cpt1 cpt2) :when (and (Cpt? cpt1) (Cpt? cpt2))))

(def a (cpt-uniform 'x 0 1))
(def b (cpt-uniform 'b 0 1))
(def c (cpt-uniform 'c 1 2))

(def the-sum (apply-binary-cpt + b c '(+ b c)))

(def x (apply-binary-cpt + the-sum b '(+ b (+ b c))))

(def z (cpt-uniform 'z 2 3))

(def p (apply-binary-cpt * z x '(* z x)))
(sum-to-one? p)
;; ;; Conditional Expectation ====================================================
;; (defn unnormalised-cond-dist
;;   "Conditional expectation of rv given pred?
;;    Currently only supports hard constraints on random variable itself"
;;   [pred? table]
;;   (m/matrix (filter #(pred? (last %)) (m/rows table))))

;; (defn cond-dist
;;   "Conditional distribution of rv given pred?
;;    Currently only supports hard constraints on random variable itself"
;;   [table pred?]
;;   (normalize (unnormalised-cond-dist pred? table)))

;; (defn cond-exp
;;   [pred? table]
;;   (println (pred? 3)))
;; ;;   (m/esum (first (m/columns (unnormalised-cond-dist pred? table)))))

;; ;; Rules ======================================================================
;; (defrule uniform-int
;;   "Discrete Uniform Contructor"
;;   (-> (uniform-int var-name x y) (cpt-uniform var-name x y)))

;; (defrule conditional-expectation
;;   "Conditional Expectation Rule"
;;   (-> (cond-exp pred? X) (cond-exp pred? (:entries X)) :when (Cpt? X)))

;; (def rules (concat std-rules [uniform-int conditional-expectation]))
;; (def eager-transformer (partial transformer/eager-transformer rules))
;; (def a (sigma-rewrite '(uniform-int 'x 0 4) eager-transformer))

;; (sigma-rewrite '(cond-exp (fn [x] > x 2) (uniform-int 'x 1 3)) eager-transformer)

;; (defn normalize
;;   "Normalise a discrete distribution"
;;   [cpt-matrix]
;;   (let [[probs & values] (m/columns cpt-matrix)]
;;     (m/matrix (concat [(map #(/ % (clzn/sum probs)) probs)]
;;             values))))
