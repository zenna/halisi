(ns relax.graphics
  (:use clozen.helpers))

(defn non-neg?
  [n]
  (>= n 0))

(defn non-pos?
  [n]
  (<= n 0))

(defn conv-cart-polar
  "Convert a point to polar coordinates around a pole"
  [pole point]
  (let [trans-point (vec-f - point pole)
        x (first trans-point)
        y (second trans-point)
        r (Math/sqrt (+ (* x x) (* y y)))
        theta (Math/atan2 y x)]
    [r theta]))

(defn convex-hull-gf
  "Find convex hull of points with gift-wrapping algorithm"
  [points]
  (cond
    (<= (count points) 3)
    points

    :else
    (let [bottom-left-comp (comparator (fn [a b]
                                        (let [xa (first a)
                                              ya (second a)
                                              xb (first b)
                                              yb (second b)]
                                        (cond
                                          (< ya yb) true
                                          (and (= ya yb) (< xa xb)) true
                                          :else false))))
          sorted-points (sort bottom-left-comp points)
          initial-point (first sorted-points) ; start with bottom leftmost point

          ; There are two criteria for selecting a new point, before we reach the apex
          smallest-pos-val (fn [polar-coords] (let [pos-points (filter #(non-neg? (second %)) polar-coords)]
                                                (first (sort-by second < pos-points))))
          ; And after reaching the apex
          largest-neg-val (fn [polar-coords] (let [neg-points (filter #(neg? (second %)) polar-coords)]
                                                (first (sort-by second < neg-points))))]

      (loop [current-point initial-point convex-set [initial-point] find-point smallest-pos-val reached-apex false]
        (let [cart-to-polar (reduce merge
                                    (map (fn [point] {(conv-cart-polar current-point point)  point})
                                         (remove #(= current-point %) sorted-points)))
              polar-coords (extract cart-to-polar key)
              polar-angles (extract polar-coords second)
              reached-apex (or reached-apex
                               (every? non-pos? polar-angles))
              find-point (if reached-apex
                            largest-neg-val
                            smallest-pos-val)

              next-point-polar (find-point polar-coords)
              next-point (cart-to-polar next-point-polar)]
          (cond
            (= initial-point next-point) convex-set

            :else
            (recur next-point (conj convex-set next-point) find-point reached-apex)))))))

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

(defn point-out-poly?
  "is the point in the poly?
  True when the winding num is zero"
  [poly point]
  (zero? (winding-num poly point)))

(defn point-in-poly?
  "is the point outside the polygon?"
  [poly point]
  (not (point-out-poly? poly point)))

(defn set-complement
  [points-a points-b]
  (remove #(in? points-b %) points-a))

(defn sample-wo-replacement
  "Select n unique points"
  [coll n]
  {:pre [(>= (count coll) n)]}
  (loop [samples []]
    (let [sample (rand-nth coll)]
      (cond
        (= (count samples) n)
        samples
      
        (in? samples sample)
        (recur samples)

        :else
        (recur (conj samples sample))))))

; (defn gen-simple-poly
;   "Generate simple polygon in n-dims dimensions"
;   [n-dims]
;   (let [n-points 5;(rand-int 5)
;         pvar (println "num-points" n-points)
;         ; TODO: test for colinearity
;         points (repeatedly n-points #(repeatedly n-dims rand))
;         pvar (println "all-points:" points)
;         ; TODO handle identical points
;         initial-poly (gen-until #(sample-wo-replacement points 3)
;                                 (fn [poly] (every? #(point-out-poly? (convex-hull-gf poly) %)
;                                                     (set-complement points poly))))]
;   (loop [poly initial-poly]
;     (println "Poly is" poly)
;     (cond
;       (= (count poly) n-points) poly ; No more points to add to poly
      
;       ; Find set of points, s \in S, such that if we extend the poly to include s
;       ; all other points not in the poly, will not be in the convex hull of the poly
;       :else
;       (let [ss (filter (fn [point] (every? #(point-out-poly? (convex-hull-gf (conj poly point)) %)
;                                                       (set-complement points (conj poly point))))
;                        (set-complement points poly))
;             pvar (println "candidate points are:" ss)
;             s (rand-nth ss) ; Uniformly dist over S
;             pvar (println "new point:" s)
;             ;2. Find completely visible edge
;             ]
;         (recur poly))))))

; (def hull (convex-hull-gf [[0.0 0.0] [1.0 0.0] [1.0 1.0] [1.5 0.5] [0.0 1.0]]))

; (println "HULL IS" hull)

; (gen-simple-poly 2)

(defn edge-sum
  "Sum over edges
  If Positive, poly is clockwise (internal angles on right)
  If negative, poly is counter clockwise (internal angles on left)"
  [poly]
  (sum
    (for [pair (partition 2 1 (conj poly (first poly)))
          :let [[[x1 y1] [x2 y2]] pair]]
      (* (- x2 x1) (+ y2 y1)))))

(defn points-to-vec
  "convert pair of poitns to vector"
  [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])

(defn cross-product
  "compute cross product of two vectors"
  [[x1 y1] [x2 y2]]
  (- (* x1 y2) (* x2 y1)))

(defn triangle-area
  "Compute the area of a triangle"
  [[[x1 y1] [x2 y2] [x3 y3]]]
  (Math/abs
    (/ (+ (* x1 (- y2 y3)) (* x2 (- y3 y1)) (* x3 (- y1 y2))) 2)))

(defn z-cross-prods
  "Compute the z component of the cross products of edges of a polygon"
  [poly]
  (for [triple (partition 3 1 (conj poly (first poly) (second poly)))
        :let [[p1 p2 p3] triple
               v1 (points-to-vec p1 p2)
               v2 (points-to-vec p2 p3)
               ]]
    (cross-product v1 v2)))

(defn soft-convexity
  "An unnormalised continuous measure of convexity
  based on the area of triangles of offending (internal
  angle > 180) points.
  Could normalise it by the area of polygon

  If the edge-sum is negative left hand turns are internal angles
  If the sign of the cross product is Positive its a left turn"
  [poly]
  {:pre [(vector? poly)]}
  (let [orient (edge-sum poly)]
    (sum
      (for [triple (partition 3 1 (conj poly (first poly) (second poly)))
            :let [[p1 p2 p3] triple
                   v1 (points-to-vec p1 p2)
                   v2 (points-to-vec p2 p3)
                   ; p (println "C" (cross-product v1 v2))
                   ; o (println "area" (triangle-area triple))
                   ]]
        (if (= (sign (cross-product v1 v2)) (sign orient))
          (triangle-area triple)
          0)))))

(defn simple-convex?
  "Is a poylgon convex?
  Should be all left (or all right) hand turns

  May fail is polygon is complex"
  [poly]
  {:pre [(vector? poly)]}
  (consistent? sign
    (filter #(not (zero? %))
    (for [triple (partition 3 1 (conj poly (first poly) (second poly)))
          :let [[p1 p2 p3] triple
                 v1 (points-to-vec p1 p2)
                 v2 (points-to-vec p2 p3)
                 ]]
      (cross-product v1 v2)))))

(defn conv-polar-range
  [[r theta]]
  (if (neg? theta)
    [r (+ theta (* 2 Math/PI))]
    [r theta]))

(defn convex?
  "Is a polygon convex?
  Works whether poly is simple or complex"
  [poly]
  (sum
  (map (fn [cross-prods]
          (let [filtered (filter #(not (zero? %)) cross-prods)]
            (if (consistent? sign filtered)
                0
                (max (count (filter neg? filtered)) (count filtered)))))
  (for [pair (partition 2 1 (conj poly (first poly)))
        :let [[p1 p2] pair]]
    (map #(cross-product
            (points-to-vec p1 p2)
            (points-to-vec p2 %))
      (filter #(and (not= p1 %) (not= p2 %)) poly))))))

(defn half
  [x]
  (/ x 2))

(defn gen-unconstrained-poly
  "Generate an unconsrained polygon"
  [max-width max-height n-points]
  (vec (repeatedly n-points
              (fn [] (vec (repeatedly 2 #(* (min max-width max-height) (rand))))))))

(defn gen-convex-poly
  "Generate a convex polygon"
  [max-width max-height n-points]
  (let [unconstrained-poly (gen-unconstrained-poly max-width max-height n-points)
        convex-poly (convex-hull-gf unconstrained-poly)]
    convex-poly))

(defn gen-half-screen-poly
  [max-width max-height]
  [[(half max-width) 0] [(half max-width) max-height] [max-width max-height] [max-width 0]])