(ns ^{:doc "Generate (e.g. sample) Geometry"
      :author "Zenna Tavares"}
  sigma.geometry.gen
  (:require [clozen.helpers :as clzn :refer :all])
  (:require [sigma.geometry.common :refer :all]
            [sigma.geometry.convex :refer [convex-hull-gf]]))

(defn half
  [x]
  (/ x 2))

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

(defn gen-unconstrained-poly
  "Generate an unconsrained polygon"
  [max-width max-height n-points]
  (vec (repeatedly n-points
         (fn [] (vec (repeatedly 2 #(* (min max-width max-height) (rand))))))))

(defn gen-convex-poly
  "Generate a convex polygon by generating uncosntrained poly and applying
   convex hull"
  [max-width max-height n-points]
  (let [unconstrained-poly (gen-unconstrained-poly max-width
  						     max-height n-points)
        convex-poly (convex-hull-gf unconstrained-poly)]
    convex-poly))

(defn gen-half-screen-poly
  [max-width max-height]
  [[(half max-width) 0] [(half max-width) max-height] [max-width max-height] [max-width 0]])