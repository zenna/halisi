(ns relax.box ^{:doc "Box abstractions"
                :author "Zenna Tavares"})

;; Box (Orthotope) abstractions
(defn middle-split
  [box]
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
  (split box (middle-split (:internals box))))

(defn bound-clause
  [clause vars]
  ; (println "clause" clause "vars" vars)
  (let [interval-constraints (map #(evalcs % the-global-environment)
                                   (vec 
                                    (reduce concat (map #(vector `(~'> ~% 0) `(~'< ~% 10)) vars))))
        ; pvar (println "interval constraints" interval-constraints)
        box (make-abstraction
              (partition 2
                         (bounding-box-lp
                           (map #(ineq-as-matrix % vars)
                                 (concat clause interval-constraints))
                            vars))
              (unsymbolise clause))]
    (if (some nil? (flatten (:internals box)))
        'empty-abstraction
        box)))

(defn abstraction-vertices
  "Get the vertices of an abstraction"
  [box]
  (apply combo/cartesian-product (:internals box)))

; THIS IS INCORRECT
(defn completely-within?
  [box vars]
  (every? #(satisfiable? % (:formula box) vars) (abstraction-vertices box)))

(defn on-boundary?
  [box vars]
  (not (completely-within? box vars)))

(defn volume
  "get the box volume"
  [box]
  (apply * (map #(- (upper-bound %) (lower-bound %)) (:internals box))))

(defn abstraction-sample
  "Sample with the abstraction"
  [box]
  (for [interval (:internals box)]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

;; Boolean Operations

;TODO
(defn overlap
  "Compute overlapping hyperrectangle from two overlappign ones"
  [box1 box2]
  ; (println "Boxes" box1 box2)
  {:formula #(and (apply (:formula box1) %) (apply (:formula box2) %))
   :internals
    (vec
      (for [[[low1 high1][low2 high2]]
            (partition 2 (interleave (:internals box1) (:internals box2)))]
        [(max low1 low2)(min high1 high2)]))})

(defn expand-box
  [box other-boxes]
  ;1. Choose a dimension.
  ;2. Sort the boxes according to that dimension
  ; (memoize this so it only needs to be done d times)
  ;3. Starting at self, and increasing with boxes in correct dimension
  ; ask if I were to extend current box here would it be satisfiable
  ; Would it collide any other boxes
  ; How to check satisfiable
  ; Checking collisions is an intersect check for all other boxes, linear in worst case 
  ; And could do better using a spatial data structure if necessary
  ; 

(defn cover-abstr
  "Given"
  [& boxes]
  1. Get the initial box1.
  Now what I need to expand the box.
  ;Expand box to 