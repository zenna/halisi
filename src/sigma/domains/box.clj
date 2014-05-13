(ns ^{:doc "Axis Aligned Box (Orthorope) abstractions"
                :author "Zenna Tavares"}
  sigma.domains.box
  (:require [sigma.abstraction :refer :all]
            [clozen.profile :refer :all]
            [clozen.helpers :refer :all]
            [clojure.math.combinatorics :as combo]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]))

; ;;
; The profiling tells us,
; The run time grows exponentially with the number of calls to expand
; -This means that more time is being spent in each call to expand as the number of boxes is increasing

; The number of calls to expand grows exponentially then linearly with the number of boxes in the output
(defn box
  "Box Constructor"
  [intervals]
  (make-abstraction intervals 'no-formula))

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
  "Find middle points on each axis"
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
  "Split box into 2^d equally sized boxes by cutting down middle of each axis"
  [box]
  (split box (middle-split box)))

(defn abstraction-vertices
  "Get the vertices of a polyhedral abstraction"
  [box]
  (apply combo/cartesian-product (:internals box)))

; FIXME: THIS IS INCORRECT
(defn completely-within?
  [box vars]
  false)
  ; (every? #(satisfiable? % (:formula box) vars) (abstraction-vertices box)))

; FIXME: AND HENCE THIS IS INCORRECT
(defn on-boundary?
  [box vars]
  (not (completely-within? box vars)))

(defn not-intersect?
  "Do two boxes not intersect in cartesian space"
  [box1 box2]
  ; (println box1 "!!!" box2)
  (let [x
        (for [dim (range (num-dims box1))]
              (or (<= (upper-bound (nth-dim-interval box1 dim))
                     (lower-bound (nth-dim-interval box2 dim)))
                  (>= (lower-bound (nth-dim-interval box1 dim))
                     (upper-bound (nth-dim-interval box2 dim)))))]
        (some true? x)))

(defn intersect?
  [box1 box2]
  "Do they intersect?"
  (not (not-intersect? box1 box2)))

(defn point-in-abstraction?
  "Is a point within a box"
  [box point]
  {:pre [(count= (:internals box) point)]}
  ; (println "PIA" point box (intersect? box (make-abstraction (mapv #(vector % %) point) 'no-formula)))
  ; Assume point is degenerate box
  (intersect? box (make-abstraction (mapv #(vector % %) point) 'no-formula)))

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
  "Compute overlapping hyperrectangle from two overlapping ones"
  [box1 box2]
  (if (intersect? box1 box2)
      ;FIXME: This is not incorrect due to let form.
      {:formula #(and (apply (:formula box1) %) (apply (:formula box2) %))
       :internals
        (vec
          (for [[[low1 high1][low2 high2]]
                (partition 2 (interleave (:internals box1) (:internals box2)))]
            [(max low1 low2)(min high1 high2)]))}
      'empty-abstraction))

(defn volume
  "Get the box volume"
  [box]
  (apply * (map #(- (upper-bound %) (lower-bound %)) (:internals box))))

(defn union-volume
  "Find the volume of the union of a set of boxes
   Recurse through boxes adding on volume of each new one seen
   and subtracting union volume of interection of new one and all
   previous ones"
  [& boxess]
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
  (interval-sample (:internals box)))

;; Boolean Operations
(defn remove-dim
  "Remove a dimension from a box"
  [box dim]
  (assoc box :internals (vec-remove (:internals box) dim)))

(defn edit-interval
  "Change the interval at a particular dimension"
  [box dim interval]
  (assoc-in box [:internals dim] interval))

(defn edit-lower-bound
  "Change hte lower bound at a particular dimension"
  [box dim new-bound]
  (assoc-in box [:internals dim 0] new-bound))

(defn edit-upper-bound
  "Change the upper bound at a particular dimension"
  [box dim new-bound]
  (assoc-in box [:internals dim 1] new-bound))

; FIXME, IM SAMPLING WITHIN THE ENTIRE EXPANDED BOX NOT JUST THE EXPANDED REGION
(defn valid-ext
  "LOLZ!"
  [box exp-box boxes]
  ; (println "ext" exp-box)
  (let [n-samples 100000]
    (loop [n-samples n-samples]
      ; (println n-samples)
      (cond
        (zero? n-samples)
        true

        :else
        (let [sample (abstraction-sample exp-box)]
          (if (p :pia (some #(point-in-abstraction? % sample) boxes))
              (recur (dec n-samples))
              false))))))

(defn collides?
  "Does this box collide with any of the boxes?"
  [box boxes]
  (some #(intersect? box %) boxes))

; TODO, AVOID SELF INTERSECTION
(defn expand-box
  "Expand a box into greedily provided it is contained within a union of boxes
   box - box to expand
   boxes - set of boxes we are trying to cover
   cover - cover created so far, i.e. regions to avoid.
   dim-order - what order of dimension should we expand in"
  [box boxes cover dim-order]
  (let [n-dims (num-dims box)]
  ; (println "\n expanding box" box "n in cover" (count cover) "dim order" dim-order)
  (loop [exp-box box sides dim-order]
    (cond
      (empty? sides)
      exp-box

      :else
      (let [dim (first sides)
            ; pvar (println "dim"  dim)
            interval (nth-dim-interval box dim)
            ; We only care about a subset of boxes:
            ; Ignore dim in intersection? because even though exp-box may not
            ;  intersect in dim with a box-a in boxes, may be possible for
            ;  exp-box to expand into box-a through other boxes.
            ; Complexity: n*d
            good-boxes (p :filter-good
            (filter #(intersect? (remove-dim exp-box dim)
                                 (remove-dim % dim))
                    boxes))
            ; pvar (println "good boxes" good-boxes)

            ; Flatten boxes to possible extension points in dim:
            ; Complexity- n + that of flatten
            ext-points (p :sort (sort
            (flatten (map #(nth-dim-interval % dim) good-boxes))))

            ; pvar (println "ext points" ext-points)
            ; pvar (println "filtered-high" (filterv #(>= % (upper-bound interval)) ext-points))
            ; pvar (println "filtered-low" (filterv #(<= % (lower-bound interval)) ext-points))

            ; pvar (println "collides" (collides? exp-box cover))
            ; collision: n (check every other box) * d (collison involves checking every dimension) * n
            ; in the worse case we have to check every box

            ; Find greatest extension which a) does not intersect with cover
            ; b) is fully contained within the union
            upper (p :upper
            (max-pred #(and (not (p :collides (collides? (edit-upper-bound exp-box dim %) cover)))
                            (valid-ext exp-box
                                       (edit-upper-bound exp-box dim %)
                                       boxes))
                      (filterv #(>= % (upper-bound interval)) ext-points)))
            ; pvar (println "upper" upper)

            lower
            (min-pred #(and (not (collides? (edit-lower-bound exp-box dim %) cover))
                            (p :valid (valid-ext exp-box
                                       (edit-lower-bound exp-box dim %)
                                       boxes)))
                      (filterv #(<= % (lower-bound interval)) ext-points))
            ; pvar (println "lower" lower)
            new-upper (if (nil? upper) (upper-bound interval) upper)
            new-lower (if (nil? lower) (lower-bound interval) lower)
            ; pvar (println "newlowerupper" new-lower new-upper)
            ]
        (recur (edit-interval exp-box dim [new-lower new-upper]) (rest sides)))))))

(defn box-in-union
  [boxes]
  "Give me an arbitrary box inside the union."
  (first boxes))

(defn side-to-box
  "Return a degenerate box from a side"
  [[side-box dim lower-upper]]
  (update-in side-box [:internals dim] #(vector (nth % lower-upper)
                                                (nth % lower-upper))))
;TODO
(defn intersecting-components
  "Partition set of possibly overlapping boxes into subsets of connected
   components:  Naive n^2 algorithm"
  [boxes]
  (loop [ccs [] boxes boxes]
    (if
      (empty? boxes) ccs
      (if-let [comp-i (first-index-pred #(collides? (first boxes) %) ccs)]
        (recur (update-in ccs [comp-i] #(conj % (first boxes)))
               (rest boxes))
        (recur (conj ccs [(first boxes)]) (rest boxes))))))

(defn side-extensions
  "Returns a mapping from sides of a box to possible extensions"
  [[side-box dim lower-upper :as side] boxes]
  (let [;pvar (println "FILTER NOTYET  BOXS" side)
        valid-boxes (filter #(intersect? % (side-to-box side)) boxes)
        side-pos (nth (nth-dim-interval side-box dim) lower-upper)]
    ; Flatten the overlap between a boxes into side
    (mapv #(assoc-in (overlap side-box %)
                     [:internals dim]
                     [side-pos side-pos])
            valid-boxes)))

(defn enum-sides
  "Return the sides of a box
   A side is of the form [box dim 0|1 (lower or upper)]"
  [box]
  (reduce concat
    (mapv #(vector [box % 0] [box % 1])
        (range (count (:internals box))))))

(defn box-extensions
  "For each side of box returns a pair
  [side [box-components]] where an extension is a degenerate box"
  [box boxes]
  (o :side-exts-counts count
    (vec (mapv #(update-in % [1] intersecting-components)
             (remove #(empty? (second %))
                     (mapv #(vector % (side-extensions % boxes))
                           (enum-sides box)))))))

(defn side-dim
  "What dim is the side on"
  [side]
  (nth side 1))

; NO TEST
(defn cover-connected-abstr
  "Same as cover-abstr, but assumes intersection graph of boxes is connected.
   Dissect boxes into an non overlapping set covering the same area.
   Assumes all boxes same dim"
  [boxes]
  ; (println "BOXES ARE" boxes "NUM" (count boxes))
  (let [n-dims (num-dims (box-in-union boxes))
        init-box (expand-box (box-in-union boxes) boxes []
                             (range n-dims))
        exts (box-extensions init-box boxes)]
  (loop [exts exts cover [init-box]]
    ; (println "  **ATTEMPTING EXTENSION, NUM IN COVER" (count cover) "NUM IN exts" (count exts))
    (if (empty? exts)
        cover
        (let [;pvar (println "EXTENSIONS" (map #(get-in % [1 0]) exts))
              [[side components :as ext] popd-exts] (rand-vec-remove exts)
              ; pvar (println "comp counts" (count components))
              [component popd-components] (rand-vec-remove components)
              order (concat [(side-dim side)]
                           (shuffle (remove #(= (side-dim side) %)
                                            (range n-dims))))
              order [(side-dim side)] ;TEST
              box (p :expand (expand-box (rand-nth component) boxes cover order))
              exts (if (empty? popd-components)
                        popd-exts
                        (conj popd-exts [side popd-components]))
              ; pvar (println "NEW-EXTENSIONS" (map #(get-in % [1 0])
              ;   (box-extensions box boxes)))
              ; pvar (println "HAS VOLUME" (not (tolerant= 0.0 (volume box))))
              ]
          (if (not (tolerant= 0.0 (volume box)))
              (recur (vec (concat exts (p :extend (box-extensions box boxes))))
                     (conj cover box))
              (recur exts cover)))))))

(defn cover-abstr
  "Dissect boxes into an non overlapping set covering the same area.

  Incremenetal algorithm, starting with box within boxes:
  1. Expand box, add it's sides to a set of unvisited-sides
  2. "
  [boxes]
  (let [ccs (o :count-ccs count (intersecting-components boxes))
        cover (reduce concat (mapv cover-connected-abstr ccs))]
    (println "expanding " (count boxes) " n-ccs "
             (count ccs) " in dims " (num-dims (first boxes)) "cover-count" (count cover))
    (println "union" (apply union-volume boxes) "sum volume" (sum (map volume boxes)))
    cover))

(defn gen-random-boxes
  [n-dims n-boxes]
  {:post [(do (println "POST n-dims" (num-dims (first %)) "n-boxes" (count %)
              "ccs" (map count (intersecting-components %))
              "total vol" (sum (map volume %))) true)]}
  (println "n-dims" n-dims "n-boxes" n-boxes)
  (let [side-len (/ (Math/pow (/ 1.0 (* n-dims n-boxes)) (/ 1.0 n-dims)) 2.0)]
    (repeatedly n-boxes
      #(make-abstraction
        (vec (for [dim (range n-dims)
                  :let [mid (rand)]]
             [(- mid side-len) (+ mid side-len)]))
        'no-formula))))

(defn tile-boxes
  [n-dims n-boxes]
  (let [side-len (Math/pow (/ 1.0 (* n-dims n-boxes)) (/ 1.0 n-dims))
        inter-dist (* side-len 2.5)
        n-per-side (Math/ceil (Math/pow n-boxes (/ 1.0 n-dims)))]
    (mapv
      (fn [coord-indices]
        (make-abstraction
          (mapv #(vector
                   (* % inter-dist)
                   (+ (* % inter-dist) side-len))
            coord-indices)
          'no-formula))
      (apply combo/cartesian-product (repeat n-dims (range n-per-side))))))

(comment
  ;; Some test boxes
  (def b1 {:internals (vec (repeat 2 [0 5]))})
  (def b2 {:internals (vec (repeat 2 [3 10]))})
  (def b1x {:internals (vec (repeat 2 [20 25]))})
  (def b2x {:internals (vec (repeat 2 [23 30]))})

  (def b3 {:internals (vec (repeat 6 [0 20]))})
  (def b4 {:internals (vec (repeat 6 [8 14]))})
  (def b5 {:internals (vec (repeat 6 [12 20]))})
  (def b6 {:internals (vec (repeat 6 [19 24]))})

  (defn two-boxes [x y ]
    [b1 b2 b1x b2x])

  (defn tt [x]
    (o :n-boxes count (cover-abstr x)))

  (scaling tt gen-random-boxes [[2 10][3 10]] 5)

  (scaling cover-abstr gen-random-boxes [[1 1][1 2][1 3][1 4][1 5]] 2)

  (get-scaling-data (ok) 0 [:whole :mean])

  (defn a-test []
    ; (union-volume b1 b2 b3))
    (count (cover-abstr [b1 b2 b3 b4 b5 b6])))

  (defn a-test []
    ; (union-volume b1 b2 b3))
    (profile :info :Arithmetic
    (count (cover-abstr (gen-random-boxes 2 20)))))

  (defn tests[& x]
    ; (println "IN TESTs" (num-dims (first x))))
    (println "num boxes is" (count x) "dim" (num-dims (first x))))

  (defn vol-f
    [boxes]
    (let [v1 (apply union-volume boxes)
          v2 (sum
              (map #(apply union-volume %) (intersecting-components boxes)))]
          (println "vols" v1 "-" v2)
          v1))

  (defn ok []
   (scaling #(apply union-volume %) gen-random-boxes
            (make-input 200 (positive-numbers 1) (repeat 10))
            2)))
