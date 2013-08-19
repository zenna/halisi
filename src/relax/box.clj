(ns ^{:doc "Axis Aligned Box (Orthorope) abstractions"
                :author "Zenna Tavares"}
  relax.box
  (:use relax.abstraction)
  ;(:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

(defn lower-bound [interval]
  (first interval))

(defn upper-bound [interval]
  (second interval))

(defn num-dims
  "Dimensionality of box"
  [box]
  (count (:internals box)))

(defn nth-dim-interval
  "Get the interval at particular dimenison"
  [{box :internals} dim-n]
  (nth box dim-n))

(defn middle-split
  [{box :internals}]
  (map #(double (+ (lower-bound %) (/ (- (upper-bound %) (lower-bound %)) 2))) box))

(defn split
  "Split a box into 2^D other boxes"
  [box split-points]
  (map
    #(make-abstraction % (:formula box)) ; All subboxes have same formula as parent
    (for [dim-to-change (apply combo/cartesian-product (:internals box))]
      (mapv
        (fn [dim-to-replace min-max split-point]
          (vec (sort [(first (filter #(not= dim-to-replace %) min-max)) split-point])))
        dim-to-change (:internals box) split-points))))

(defn split-uniform
  "Split the box into equally sized boxes"
  [box]
  (split box (middle-split box)))

(defn abstraction-vertices
  "Get the vertices of an abstraction"
  [box]
  (apply combo/cartesian-product (:internals box)))

; THIS IS INCORRECT
(defn completely-within?
  [box vars]
  false)
  ; (every? #(satisfiable? % (:formula box) vars) (abstraction-vertices box)))

(defn on-boundary?
  [box vars]
  (not (completely-within? box vars)))

(defn not-intersect?
  [box1 box2]
  ; (println box1 "!!!" box2)
  (let [x
        (for [dim (range (num-dims box1))]
              (or (< (upper-bound (nth-dim-interval box1 dim))
                     (lower-bound (nth-dim-interval box2 dim)))
                  (> (lower-bound (nth-dim-interval box1 dim))
                     (upper-bound (nth-dim-interval box2 dim)))))]
        (some true? x)))

(defn intersect?
  [box1 box2]
  "Do they intersect?"
  (not (not-intersect? box1 box2)))

(defn abstraction-contains?
  "Does box-a fully contain box-b?
   This is true when for each dimenison lower-bound of A is lte
   and upper-bound is gte B"
  [box-a box-b]
  ; {:pre [(count= box-a box-b)]}
  (every?
    #(and (<= (lower-bound (nth-dim-interval box-a %))
              (lower-bound (nth-dim-interval box-b %)))
          (>= (upper-bound (nth-dim-interval box-a %))
              (upper-bound (nth-dim-interval box-b %))))
    (range (num-dims box-a))))

;TODO
(defn overlap
  "Compute overlapping hyperrectangle from two overlappign ones"
  [box1 box2]
  (if (intersect? box1 box2)
      {:formula #(and (apply (:formula box1) %) (apply (:formula box2) %))
       :internals
        (vec
          (for [[[low1 high1][low2 high2]]
                (partition 2 (interleave (:internals box1) (:internals box2)))]
            [(max low1 low2)(min high1 high2)]))}
      'empty-abstraction))

(defn volume
  "get the box volume"
  [box]
  (apply * (map #(- (upper-bound %) (lower-bound %)) (:internals box))))

(defn union-volume
  "Find the volume of the union of a set of boxes
   Recurse through boxes adding on volume of each new one seen
   and subtracting union volume of interection of new one and all
   previous ones"
  [& boxess]
  ; (println boxess)
  (if (or (empty? boxess) (nil? boxess))
    0.0
    (loop [boxes (rest boxess) seen-boxes (list (first boxess))
           vol (volume (first boxess))]
      ; (println "vol is" vol)
      ; (println "box is" (first boxess))
      (if (empty? boxes)
          vol
          (recur (rest boxes)
                 (conj seen-boxes (first boxes))
                 (- (+ vol (volume (first boxes)))
                    (apply union-volume
                           (filter has-volume?
                                   (map #(overlap % (first boxes))
                                        seen-boxes))))))))) 

;TODO Does this support negative intervals?
(defn interval-sample
  "Sample within box"
  [intervals]
  (for [interval intervals]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

(defn abstraction-sample
  "Sample with the abstraction"
  [box]
  (for [interval (:internals box)]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

;; Boolean Operations



; (defn expand-box
;   [box other-boxes]
;   ;1. Choose a dimension.
;   ;2. Sort the boxes according to that dimension
;   ; (memoize this so it only needs to be done d times)
;   ;3. Starting at self, and increasing with boxes in correct dimension
;   ; ask if I were to extend current box here would it be satisfiable
;   ; Would it collide any other boxes
;   ; How to check satisfiable
;   ; Checking collisions is an intersect check for all other boxes, linear in worst case 
;   ; And could do better using a spatial data structure if necessary
;   ; 

; (defn cover-abstr
;   "Given"
;   [& boxes]
;   1. Get the initial box1.
;   Now what I need to expand the box.
;   ;Expand box to 


; (defn tile-abstr
;   "Box"
;   [& boxes]
;   ; {:pre [(true? (apply count= boxes))]} ; All boxes have same dim
;   (let [;boxes (map :internals boxes)
;         n-dim (num-dims (first boxes))
;         planes
;         (for [dim (range n-dim)]
;           (partition 2 1
;             (sort (distinct (reduce concat (map #(nth % dim) boxes))))))]
;     ; Retain cells which are contained by one of the orig Boxes
;     (filter
;       #(some true? (for [box boxes] (contains? box %)))
;       (apply combo/cartesian-product planes))))

(def b1 {:internals [[0 5][0 5]]})
(def b2 {:internals [[3 6][2 10]]})
(def b3 {:internals [[0 10][0 10]]})

(defn gen-random-boxes
  [n-dims n-boxes]
  (repeat n-boxes
    (vec (for [dim (range n-dims)]
         [[(rand) (rand)][(rand) (rand)]]))))

(defn -main []
  (union-volume b1 b2 b3))