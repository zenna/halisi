(ns ^{:doc "Evaluation Rewrite Rules"
      :author "Zenna Tavares"}
  sigma.rules
  (:require [veneer.pattern.dsl :refer [defrule]]
            [veneer.pattern.rule :refer :all]
            [veneer.pattern.match :refer :all])
  (:require [clozen.iterator]))

;; Abstract Operators
; Random Primitives
(defrule
  "Interval abstraction of uniform real"
  (⊑ (rand) (box [[0 1]])))

(defrule
  "Interval abstraction of uniform real"
  (⊑ (rand-int low up) (interval-abo low up)))

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

