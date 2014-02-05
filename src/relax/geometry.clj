(ns ^{:doc "Geometry"
	    :author "Zenna Tavares"}
  relax.geometry
  (:use clozen.helpers))

;; Non-linear planning constraint
(defn point-in-box?
  "Is a point inside a box?"
  [[[px py] :as point] [[[low-x up-x][low-y up-y]] :as box]]
  (and
    (>= px low-x)
    (<= px up-x)
    (>= py low-y)
    (<= py up-x)))

(defn points-to-vec
  "Convert from points representation to vector representation"
  [[p1x p1y] [p2x p2y]]
  [(- p1x p2x)(- p1y p2y)])

(defn parametric-to-point
  "A line can be represented parametrically as a point
   x = x_0 + a*t"
  [[ax ay] [bx by] t]
  [(+ ax (* (- bx ax) t))
   (+ ay (* (- by ay) t))])

(defn intersection-point
  "Find the intersection point of two 2D vectors.
   Each vector is defined as a pair of points, [a b].
   returns t parameter on line [a0 a1].
   fraction of distane from a0 to a1.
   
   TODO: return :parallel if parallel"
  [[a0 a1] [b0 b1]]
  (let [[u1 u2] (points-to-vec a0 a1)
        [v1 v2] (points-to-vec b0 b1)
        [w1 w2] (points-to-vec b0 a0)]
    (/ (- (* v2 w1) (* v1 w2))
       (- (* v1 u2) (* v2 u1)))))

(comment
  (require '[relax.geometry :refer :all])
  (def vec-a [[1 1] [5 5]])  ;; segment on y = x 
  (def vec-b [[0 4.9] [10 4.9]]) ;; segment on y = 4.5
  (intersection-point vec-a vec-b) ;; => 0.5
  (= (intersection vec-a vec-b) (intersection vec-b vec-a)) ; => false - not symmetric
  (parametric-to-point (first vec-a) (second vec-a)  (intersection-point vec-a vec-b)) ; => 4.9
)