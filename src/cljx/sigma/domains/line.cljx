  (ns ^{:doc "A line"
      :author "Zenna Tavares"}
  sigma.domains.line
  (:require [clozen.geometry.line :refer [->Line]]
            [clozen.vector :refer [->VectorN s* add-scaled-v v-]]
            [veneer.pattern.dsl-macros :refer [defrule]]))

(defrecord PiecewiseLinear
  [points])

(defn linear-interpolate
  "Linearly interpolate between two points to produce a path of"
  [p0 pn n-points]
  (let [delta-v (s* (double (/ 1 (inc n-points))) (v- pn p0))]
    (mapv #(add-scaled-v p0 delta-v %) (range 0 (+ n-points 2)))))

(defn ->PiecewiseLinear
  "We will represent a distribution as a set of piecewise linear path"
  [points]
  (PiecewiseLinear. points))

(defn build-uniform-dist
  "Create a uniform distribution"
  [a b n]
  {:pre [(> b a)]}
  (let [height (/ 1 (- b a))]
    (->PiecewiseLinear (linear-interpolate (->VectorN 2 [a height])
                                           (->VectorN 2 [b height]) n))))

(defrule uniform
  "Define Rule"
  (-> (uniform a b) (build-uniform-dist a b 2)))

