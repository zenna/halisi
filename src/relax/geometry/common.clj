(ns ^{:doc "Geometry"
      :author "Zenna Tavares"}
  relax.geometry.common
  (:require [clozen.helpers :as clzn :refer :all]))

(defn non-neg?
  [n]
  (>= n 0))

(defn non-pos?
  [n]
  (<= n 0))

;; Vector Operations
(defn conv-cart-polar
  "Convert a point to polar coordinates around a pole"
  [pole point]
  (let [trans-point (vec-f - point pole)
        x (first trans-point)
        y (second trans-point)
        r (Math/sqrt (+ (* x x) (* y y)))
        theta (Math/atan2 y x)]
    [r theta]))

(defn points-to-vec
  "convert pair of points to vector"
  [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])

(defn perp-2d
  "2D perp operator"
  [[x y :as vect]]
  {:pre [(= 2 (count vect))]}
  [(- y) x])

(defn cross-product
  "Compute cross product of two vectors"
  [[x1 y1] [x2 y2]]
  (- (* x1 y2) (* x2 y1)))

(defn z-cross-prods
  "Compute the z component of the cross products of edges of a polygon"
  [poly]
  (for [triple (partition 3 1 (conj poly (first poly) (second poly)))
        :let [[p1 p2 p3] triple
               v1 (points-to-vec p1 p2)
               v2 (points-to-vec p2 p3)
               ]]
    (cross-product v1 v2)))

(defn conv-polar-range
  [[r theta]]
  (if (neg? theta)
    [r (+ theta (* 2 Math/PI))]
    [r theta]))

(defn add-vec
  "Add a vector to a point"
  [point vect]
  (mapv + point vect))

(defn parametric-to-point
  "A line can be represented parametrically as a point
   x = x_0 + a*t.
   Given a line segment as two points and t value,
   return point along this line."
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
    (when (zero? (- (* v1 u2) (* v2 u1)))
          (println "zero" [a0 a1] [b0 b1]))
    (/ (- (* v2 w1) (* v1 w2))
       (- (* v1 u2) (* v2 u1)))))

(defn gradient
  "Return the gradient of a pair of 2D points"
  [a b]
  (let [[x-delta y-delta] (points-to-vec a b)]
    (if (zero? x-delta)
        nil ; No gradient
        (/ y-delta x-delta))))

;; Polygons (i.e. 2D)
(defn winding-num
  "Return winding number of polygon
   see Alciatore "
  [poly point]
        ; translate poly such that point is at origin
  (let [translated-poly (map #(vec-f - % point) poly)]
    ; w is wind-num
    (loop [vertices translated-poly w 0]
      (cond
        (= (count vertices) 1)
        w

        :else
        (let [x1 (first (first vertices))
              x2 (first (second vertices))
              y1 (second (first vertices))
              y2 (second (second vertices))]
          (cond 
            (and (< (* y1 y2) 0)
                 (> (+ x1 (/ (* y1 (- x2 x1))
                         (- y1 y2)))
                    0))
            (if (< y1 0)
                (recur (rest vertices) (inc w))
                (recur (rest vertices) (dec w)))

            (and (zero? y1)
                 (> x1 0))
            (if (> y2 0)
                (recur (rest vertices) (+ w 0.5))
                (recur (rest vertices) (- w 0.5)))

            (and (zero? y2)
                 (> x2 0))
            (if (< y1 0)
                 (recur (rest vertices) (+ w 0.5))
                 (recur (rest vertices) (- w 0.5)))

            :else
            (recur (rest vertices) w)))))))

(defn edge-sum
  "Sum over edges
  If Positive, poly is clockwise (internal angles on right)
  If negative, poly is counter clockwise (internal angles on left)"
  [poly]
  (sum
    (for [pair (partition 2 1 (conj poly (first poly)))
          :let [[[x1 y1] [x2 y2]] pair]]
      (* (- x2 x1) (+ y2 y1)))))

(defn clockwise?
  "Does the polygon have a clockwise ordering?"
  [poly]
  (pos? (edge-sum poly)))

(defn point-out-poly?
  "is the point in the poly?
   True when the winding num is zero"
  [poly point]
  (zero? (winding-num poly point)))

(defn point-in-poly?
  "is the point outside the polygon?"
  [poly point]
  (not (point-out-poly? poly point)))

(defn point-in-box?
  "Is a point inside a box?"
  [[[px py] :as point] [[[low-x up-x][low-y up-y]] :as box]]
  (and
    (>= px low-x)
    (<= px up-x)
    (>= py low-y)
    (<= py up-x)))

(defn box-to-poly
  "Converts a box defined as interval to a polygon"
  [[[xl xu][yl yu]]]
  [[xl yl][xu yl][xu yu][xl yu]])

(defn set-complement
  [points-a points-b]
  (remove #(in? points-b %) points-a))

(defn poly-to-edges
  "Get vector of edges of a polygon (series of points)"
  [poly]
  (mapv vec (partition 2 1 (conj poly (first poly)))))

(defn v-form-to-h
  "Convert from a vertex representation of a convex polygon to a halfspace
   Vertex representation: [[x0 y0][x1 y1]...] with implicit edge between
   first and last point.
   H-form: is a0*x0 + a1*x1 < b, represented as vector [a0 a1 b]
   Assumes a counter clockwise ordering"
  [poly]
  (for [[[ax ay :as a] [bx by :as b]] (poly-to-edges poly)]
    (if-let [a1 (gradient a b)] ; There is a gradient, e.g. y = 2x + 4
      (if (pos? (- bx ax))      ; if x 
          [-1 a1 (- (* a1 ax) ay)]
          [1  (- a1) (- ay (* a1 ax))])
      (if (pos? (- by ay))      ; No gradient, e.g. x = 3
          [0 1 ax]
          [0 -1 (- ax)]))))

(defn h-form-to-string
  "A convex polyhedron in H form.  More readible and plottable
   with Mathematica"
  [h-poly]
  (let [lineq-to-string
        (fn [lineq]
          (str
            (clojure.string/join "+"
              (map #(str "x" %2 "*" %1) (pop lineq) (range (count lineq))))
            " > " (last lineq)))]
    (clojure.string/join " && " (map lineq-to-string h-poly))))

;; Points (i.e. N-D)
(defn bounding-box-points
  "Get a bounding box of a set of d dimensional points
   e.g. [[1 2 3][4 5 6][7 8 9]] => [[1 7][2 8][3 9]]"
  [points]
  (let [n-dims (count (first points))
        update-extremes
        (fn [point ineq extremes]
         "e.g. points is [0 1 10], extremes is [+inf +inf +inf], ineq <"
         (mapv #(if (ineq %1 %2) %1 %2) point extremes))]
    ; Iterate through each point and update most extreme value of each
    ; dimension seen so far
    (loop [points points mins (repeat n-dims Double/POSITIVE_INFINITY)
                         maxs (repeat n-dims Double/NEGATIVE_INFINITY)]
      (if (seq points)
          (recur (next points) (update-extremes (first points) < mins)
                               (update-extremes (first points) > maxs))
          (mapv vector mins maxs)))))

(comment
  (require '[relax.geometry :refer :all])
  (def vec-a [[1 1] [5 5]])  ;; segment on y = x 
  (def vec-b [[0 4.9] [10 4.9]]) ;; segment on y = 4.5
  (intersection-point vec-a vec-b) ;; => 0.5
  (= (intersection-point vec-a vec-b) (intersection-point vec-b vec-a)) ; => false - not symmetric
  (parametric-to-point (first vec-a) (second vec-a)  (intersection-point vec-a vec-b)) ; => 4.9

  (def poly [[0.0 0.0][1.0 5.0][7.0 3.0]])

)