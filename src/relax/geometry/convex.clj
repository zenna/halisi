(ns ^{:doc "Convexity, measures of it and convex hull algorithms"
      :author "Zenna Tavares"}
  relax.geometry.convex
  (:require [relax.geometry.common :refer :all])
  (:require [clozen.helpers :refer :all]))

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

(defn edge-sum
  "Sum over edges
  If Positive, poly is clockwise (internal angles on right)
  If negative, poly is counter clockwise (internal angles on left)"
  [poly]
  (sum
    (for [pair (partition 2 1 (conj poly (first poly)))
          :let [[[x1 y1] [x2 y2]] pair]]
      (* (- x2 x1) (+ y2 y1)))))

(defn triangle-area
  "Compute the area of a triangle"
  [[[x1 y1] [x2 y2] [x3 y3]]]
  (Math/abs
    (/ (+ (* x1 (- y2 y3)) (* x2 (- y3 y1)) (* x3 (- y1 y2))) 2)))

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

(defn convexity-measure
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

(defn convex?
  "Is a polygon convex?
   Works whether poly is simple or complex"
  [poly]
  (zero? (convexity-measure poly)))
