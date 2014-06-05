(ns ^{:doc "Common to all abstractions"
      :author "Zenna Tavares"}
  sigma.abstraction
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

(defn make-abstraction
  "An abstraction has a formula, which is purely conjunctive,
  and can be evaluated using satisfiable?.
  It also has some internal structure which depends on its type"
  [internals formula]
  {:internals internals :formula formula})

(defn formula
  "Get formula of abstraction"
  [abstraction]
  {:post [(not (nil? %))]}
  (:formula abstraction))

(defn non-empty-abstraction?
  [abstraction vars]
  "Is the box not empty? Box can be empty because we find it infeasible
   Or due to subdivison process"
  (and
    (not= abstraction 'empty-abstraction)))
    ; (some #(satisfiable? % (formula abstraction) vars) (abstraction-vertices abstraction))))

;; (defn has-volume?
;;   [abstraction]
;;   "Does the box have volume? Box may not have volume infeasible"
;;   (not= abstraction 'empty-abstraction))
