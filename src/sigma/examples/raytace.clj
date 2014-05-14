(ns sigma.examples.raytrace
  (:require [clozen.helpers :refer [tolerant=]]))

;; What's wrong with this
;; Inconsistent naming for vectors, some times use v1 v2, v w, p q
;; Pre conditions arent compile time
;; Some functions take two points as input others take two vectors, not clear
;; Inconsistent naming for 2 dimension dot2 , perp-2d
;; Destructuring not supported by sigma
;; Avoidable general recursion
;; Shorthand for anon function unsupported by sigma

(defn tolerant=
  [x y]
  (< (* (- x y) (- x y)) 0.00001))

(defn num-dims [v] (count v))

(defn parallel?
  "parallel if w1vi-wiv1=0 for all i."
  [v w]
  {:pre [(= (num-dims v) (num-dims w))
         (> (num-dims v) 1)]}
  (let [w1 (first w)
        v1 (first v)]
    (every? #(tolerant= 0.0 %) (map (fn [vi wi] (- (* w1 vi) (* wi v1)))
                                    (rest v) (rest w)))))

(defn perp-2d
  "2D perp operator"
  [[x y :as vect]]
  {:pre [(= 2 (count vect))]}
  [(- y) x])

(defn points-to-vec
  "convert pair of points to vector"
  [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])

(defn intersection-point
  "Find the intersection point of two 2D vectors.
   Each vector is defined as a pair of points, [a b].
   returns t parameter on line [a0 a1].
   fraction of distane from a0 to a1."
  [[a0 a1 :as v] [b0 b1 :as q]]
  {:pre [(not (parallel? (points-to-vec a0 a1) (points-to-vec b0 b1)))]}
  (let [[u1 u2] (points-to-vec a0 a1)
        [v1 v2] (points-to-vec b0 b1)
        [w1 w2] (points-to-vec b0 a0)
        denom (- (* v1 u2) (* v2 u1))]
    [(/ (- (* v2 w1) (* v1 w2)) denom)
     (/ (- (* u1 w2) (* u2 w1)) (- denom))]))

(defn dot2 [[v1 v2] [q1 q2]]
  (+ (* v1 v2) (* q1 q2)))

;; 2D Ray-tracing example 1 - Constrained Naive sampler =======================

;; The first ray tracing example is similar to the path planning example
;; We first take a naive generative model over paths
(defn every-conseq-n?
  "is (f x y z ..) true for all consequtive x, y, z .. in coll
   e.g. (every-conseq-n? 2 xor [true false true false])
        (every-conseq-n? 3 #(> %3 %2 %1) [1 2 3 4 5 6 7 8])"
  [n f coll]
  (every? #(apply f %) (partition n 1 coll)))

(defn uniform [min max]
  {:pre [(> max min)]}
  (+ min (rand (- max min))))

(defn uniform-path-generator
  "Create a path (sequence of points) within some rectangular region"
  [x-min x-max y-min y-max n-points]
  (repeatedly n-points (fn [] [(uniform x-min x-max)
                               (uniform y-min y-max)])))

(defn min-index [coll]
  (loop [min-seen (first coll) min-index 0 i 1 coll (rest coll)]
    (if (empty? coll)
        [min-seen min-index]
        (if (< (first coll) min-seen)
            (recur (first coll) i (inc i) (rest coll))
            (recur min-seen min-index (inc i) (rest coll))))))

(defn reflects?
  "Find closest intersection point from p0 to an obstacle, ensure  between these points"
  [obstacles p1 p2 p3]
  (let [intersection-points (mapv #(first (intersection-point [p1 p2] %)) obstacles)
        closest-o (second (min-index intersection-points))
        obstacle-vec (apply points-to-vec (nth obstacles closest-o))]
    (dot2 obstacle-vec (points-to-vec p2 p3))
    (tolerant= (dot2 (points-to-vec p1 p2) obstacle-vec)
               (- (dot2 obstacle-vec (points-to-vec p2 p3))))))

(defn path-is-ray?
  "Path is a ray with respect to set of obstacles if"
  [path]
  (every-conseq-n? 3 (partial reflects? obstacles) path))

;; Sanity tests
(comment
  (def obstacle [[0.0 5.0] [10.0 5.0]])
  (def obstacles [obstacle])
  (def path [[0.0 0.0] [5.0 5.0] [10.0 0.0]])
  (path-is-ray? obstacles path))

;; Queries
(comment
  (def ray (condition (uniform 0 10 0 10 5) (partial path-is-ray? obstacles)))
  (def path-outside-rect? [path rect]

  (def ray-outside-container (probability ray outside-container?)))

;; Ray-tracing example 2 - dadada============================================
(defn [x-min x-max y-min y-max n-points obstacles]
  "Uniformly sample start point"
  (let [p0 [(uniform x-min x-max) (uniform y-min uniform y-max)]
        p0.5 [(uniform x-min x-max) (uniform y-min uniform y-max)]
        ])
    ()
