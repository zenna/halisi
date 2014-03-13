(ns ^{:doc "Evaluation Rewrite Rules"
      :author "Zenna Tavares"}
  relax.rules
  (:require [veneer.pattern.dsl :refer [defrule]]
            [veneer.pattern.rule :refer :all]
            [veneer.pattern.match :refer :all])
  (:require [clozen.iterator]))

(defrule primitive-apply-rule
  "Apply primitive functions"
  (-> (?f & args) (apply ?f args) :when (and (primitive? f)
                                             (evaluated? args))))

(defrule compound-f-sub-rule
  "Substitute in a compound function"
  (-> (f & args) `(~(lookup-compound f) ~@args) :when (compound? f)))

(defrule sub-vars-rule
  "A variable substitution rule"
  (-> ((fn [& args] body) & params) (rewrite )

(defrule if-rule
  "Evaluated if rule"
  (-> (| (if true branch alternative)
         (if false consequent branch)) branch))

(defrule defn-rule
  "Equivalent to defn macro"
  (-> (defn name docstring args body) `(def (fn args) body)))

(defrule define-rule!
  "Define something"
  (-> (def name docstring init-value) (update-ns name init-value)))

;; Abstract Operators
; Random Primitives
(defrule
  "Interval abstraction of uniform real"
  (⊑ (rand) (interval-abo 0 1)))

(defrule
  "Interval abstraction of uniform real"
  (⊑ (rand-int x y) (interval-abo 0 1)))

(defrule
  "Symbolic abstraction of gaussian"
  (-> (bernoulli) (bernoulli 0.5)))

(defrule
  "Symbolic abstraction of gaussian"
  (⊑ (bernoulli p) (interval-abo 0 1)))

(defrule
  "Symbolic abstraction of gaussian"
  (⊑ (unif-real) (interval-abo 0 1)))

;; Conversion between abstract domains
(defrule
  "We can cover a convex polyhedra with boxes"
  (⊑ x (cover %n-boxes x) :where (convex-polytope? x)))

(defrule
  "We can cover a convex polyhedra with boxes"
  (⊑ x (cover %n-boxes x) :where (convex-polytope? x)))

;; Primitive Operators on abstract domains
(defrule +-interval-abo
  "Add two intervals"
  (⊑ (+ & args) :let [[int-args others] (separate int-abo? args)]
     [(apply add-intervals int-args) :when (empty? others)
      (+ @others (apply add-intervals int-args)) :when (seq? int-args)]))

; Add intervals to real values

; Add convex polyhedra together


;; Under approximations

;; Samples
(defrule
  "Interval abstraction of uniform real"
  (~ (rand) (rand)))

