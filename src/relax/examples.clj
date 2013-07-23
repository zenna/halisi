(ns ^{:doc "Example generative models and constraints"
	    :author "Zenna Tavares"}
  relax.examples
  (:use clozen.helpers))

;; Graphs
(defn gen-multigraph-edge-list
  "Generate a graph in edge list form:[[n1 n2],..,[n1 n2]]"
  []
  (let [num-points (rand-int 10)
        num-edges (rand-int (* num-points num-points))]
    (for [i (range num-edges)]
      [(rand-int num-points) (rand-int num-points)])))

; NOTEST
(defn gen-graph
  "Generates a graph"
  []
  (let [num-points (rand-int 100)
        num-edges (rand-int (* num-points num-points))
        edges (vec (repeat num-points []))]
    (pass
      (fn [elem edges]
        (let [new-edge
              ; Generate an edge that doesnt already exist
              (gen-until #(vector (rand-int num-points) (rand-int num-points))
                         #(not (in? (nth edges (first %)) (second %))))
              new-val (conj (nth edges (first new-edge)) (second new-edge))]
          (assoc edges (first new-edge) new-val)))
      edges
      (range num-edges))))

(defn num-nodes
  [graph]
  (count graph))

(defn graph-is-empty?
  [graph]
  (every? empty? graph))

; NOTEST
(defn acyclic?
  "Check whether a DAG has no cycles by determining whether it can be
   topologically sorted

   Assumes graphs is in integer form"
  [graph]
  (let [sorted-elems []
        n-nodes (num-nodes graph)
        working-set (set (range n-nodes))
        working-set (pass (fn [tail working-set]
                              (disj working-set tail))
                          working-set
                          (reduce concat graph))]
    (cond
      (empty? working-set)
      false

      :else
      (graph-is-empty?
        (loop [working-set working-set sorted-elems [] graph graph]
          (cond
            (empty? working-set)
            graph

            :else
            (let [n (first working-set)
                  tails (nth graph n)
                  graph (assoc graph n [])
                  working-set (pass (fn [tail working-set]
                                        (if (in? (reduce concat graph) tail)
                                             working-set
                                            (conj working-set tail)))
                                    working-set
                                    tails)]
                  (recur (disj working-set n) (conj sorted-elems n)
                                              graph))))))))
(defn test-rejection-acyclic []
  (counts (map acyclic? (repeatedly 1000 gen-graph))))

;; Planning
(defn gen-path
  "Generate a random path in X Y"
  []
  (let [num-points (rand-int 10)]
    (repeatedly num-points #(vector (rand) (rand)))))

(defn gen-path-parametric
  "Generate a random path in X Y"
  [num-dim num-points]
  (repeatedly num-points #(vec (repeatedly num-dim rand))))

; (defn avoids-obstacles?
;   "Obstacles are of form [poly poly poly]"
;   [path obstacles]
;   (for [edge (partition 2 1 path)
;         :let [tundot (somecomp)
;               obs-edges ()]
;         obs-edges]
;     (let [t (dot-prod t-undot (edge-vec obs-edges))]
;       (or (> t 1) (< t 0)))))

(defn perp-2d
  [[x y :as vect]]
  {:pre [(= 2 (count vect))]}
  [(- y) x])

; (def obstacles [[[3 9][7 8]][[5 4][6 2]]])

; (defn create-avoid-obs
;   "Creates a program which when evaluated on a path will return true
;    only if that path passes through no obstacles
   
;    obstacles is a vector of points = []"
;   [n-points obstacles]
;   (let [vars
;         (reduce concat (for [i (range n-points)]
;           [(symbol (str "x" i)) (symbol (str "y" i))]))]
;   `(~'and
;     ~@(for [[[ob-x0 ob-y0 :as ob0][ob-x1 ob-y1 :as ob1]] obstacles
;            [path-x0 path-y0 path-x1 path-y1] (partition 4 2 vars)]
;       (let [n1 (vec-f - (perp-2d (vec-f - ob1 ob0)))
;             e_x (- ob-y1 ob-y0)
;             e_y (- ob-y0 ob-y1)
;             k (+ (* e_x ob-y0) (* e_y ob-x0))]
;         `(~'> (~'+ (~'* (~'- ~e_y) ~path-x1) (~'* (~'- ~e_x) ~path-y1)) ~k))))))

;; Inverse Graphics
; (defn gen-poly [])

; (defn simple? [])

; (defn gen-mesh [])

; (defn self-intersections? [])


;; Box packing
(defn gen-box-constraints-overlap
  [n-boxes]
  (let [vars
       (for [i (range n-boxes)]
          [(symbol (str "x" i)) (symbol (str "y" i)) (symbol (str "r" i))])]
    {:vars (reduce concat vars)
     :pred
    `(~'and
      ~@(reduce concat
        (for [[[ax ay ar][bx by br]] (unique-pairs vars)]
        `((~'< (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) 0)
          (~'> (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) 0)
          (~'> (~'+ ~ay ~ar (~'* -1 ~by) ~br ) 0)
          (~'< (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) 0)))))}))

(defn gen-box-non-overlap
  [n-boxes]
  (let [vars
       (for [i (range n-boxes)]
          [(symbol (str "x" i)) (symbol (str "y" i)) (symbol (str "r" i))])]
    {:vars (reduce concat vars)
     :pred
    `(~'and
      ~@(for [[[ax ay ar][bx by br]] (unique-pairs vars)]
        `(~'or 
          (~'> (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) 0)
          (~'< (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) 0)
          (~'> (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) 0)
          (~'< (~'+ ~ay ~ar (~'* -1 ~by) ~br ) 0))))}))

(defn gen-box-non-overlap-close
  [n-boxes]
  (let [proximity-thresh 1.0 ; How close the boxes must be
        vars
       (for [i (range n-boxes)]
          [(symbol (str "x" i)) (symbol (str "y" i)) (symbol (str "r" i))])]
    {:vars (reduce concat vars)
     :pred
    `(~'and
      ~@(for [[[ax ay ar][bx by br]] (unique-pairs vars)]
        `(~'or 
          (~'and
            (~'> (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) 0)
            (~'< (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) ~proximity-thresh))

          (~'and
            (~'< (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) 0)
            (~'> (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) ~(* -1 proximity-thresh)))

          (~'and
            (~'> (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) 0)
            (~'< (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) ~proximity-thresh))

          (~'and
            (~'< (~'+ ~ay ~ar (~'* -1 ~by) ~br ) 0)
            (~'> (~'+ ~ay ~ar (~'* -1 ~by) ~br ) ~proximity-thresh)))))}))

; the predicate is 

; (and 
;   (or 
;     (and 
;       (> (+ x0 (* -1 r0) (* -1 x1) (* -1 r1)) 0) 
;       (< (+ x0 (* -1 r0) (* -1 x1) (* -1 r1)) 1.0))
;     (and (< (+ x0 r0 (* -1 x1) r1) 0)
;          (> (+ x0 r0 (* -1 x1) r1) -1.0))
;     (and (> (+ y0 (* -1 r0) (* -1 y1) (* -1 r1)) 0)
;          (< (+ y0 (* -1 r0) (* -1 y1) (* -1 r1)) 1.0))
;     (and (< (+ y0 r0 (* -1 y1) r1) 0) (> (+ y0 r0 (* -1 y1) r1) 1.0)))

;   (or (and (> (+ x0 (* -1 r0) (* -1 x2) (* -1 r2)) 0)
;            (< (+ x0 (* -1 r0) (* -1 x2) (* -1 r2)) 1.0))
;       (and (< (+ x0 r0 (* -1 x2) r2) 0)
;            (> (+ x0 r0 (* -1 x2) r2) -1.0))
;       (and (> (+ y0 (* -1 r0) (* -1 y2) (* -1 r2)) 0)
;            (< (+ y0 (* -1 r0) (* -1 y2) (* -1 r2)) 1.0))
;       (and (< (+ y0 r0 (* -1 y2) r2) 0)
;         (> (+ y0 r0 (* -1 y2) r2) 1.0)))
  
;   (or 
;     (and (> (+ x1 (* -1 r1) (* -1 x2) (* -1 r2)) 0)
;          (< (+ x1 (* -1 r1) (* -1 x2) (* -1 r2)) 1.0))
;     (and (< (+ x1 r1 (* -1 x2) r2) 0)
;          (> (+ x1 r1 (* -1 x2) r2) -1.0))
;     (and (> (+ y1 (* -1 r1) (* -1 y2) (* -1 r2)) 0)
;          (< (+ y1 (* -1 r1) (* -1 y2) (* -1 r2)) 1.0))
;     (and (< (+ y1 r1 (* -1 y2) r2) 0)
;          (> (+ y1 r1 (* -1 y2) r2) 1.0))))


;; Random examples
(def exp 
  '(if (> x1 9)
      (or (> x2 10)
          (< x2 1))
      (if (> x2 8)
          true
          false)))

(def exp2
  '(if (> x1 8)
      (or (> x2 10)
          (< x2 1))
      (if (> x2 5)
          (or (> x2 7)
              (< x1 9))
          false)))

(def exp3
  '(or (and (> x1 7) (> x2 7) (< x1 9) (< x2 10))
       (and (> x1 3) (> x2 3) (< x1 5) (< x2 5))
       (< x1 1)))

(def exp-line
  '(if (>= (+ x2 (* -1 x1)) 0)
        true
        false))

(def exp-linear
  '(or

    (and (>= x2 9) (<= x2 10))
    (and (>= x1 3) (>= x2 3) (<= x1 5) (<= x2 5))
    (and 
      (>= x1 0)
      (>= x2 0)
      (<= (+ x2 (* (- 1) x1)) 1)
      (<= (+ x1 (* 6 x2)) 15)
      (<= (+ (* 4 x1) (* (- 1) x2)) 10))))

(def exp-linear-overlap
  '(or

    (> (+ x2 (* -1 x1)) 0)
    (and (> x1 8) (> x2 2) (< x1 10) (< x2 4))))

(def exp7
  '(if (> x1 2)
        (if (> x2 2)
            true
            false)
        false))

(def exp10
  '(if (> x1 2)
        true
        false))

(def exp4
  '(if (if (> x1 3)
            true
            false)
      false
      true))

(def exp5
  '(if (if (> x1 3)
           true
           false) 
       (if (< x2 4) true false  )
       true))

(def exp5
  '(and
    (or (> x1 1) (< x2 2) (> x1 3) (> x2 4))
    (or (> x1 5) (< x2 6) (> x1 7) (> x2 8))
    (or (> x1 9) (< x2 10) (> x1 11) (> x2 12))
    (or (> x1 13) (< x2 14) (> x1 15) (> x2 16))))

(def exp5
  '(and
    (or (> x1 1) (< x2 2))
    (or (> x1 5) (< x2 6))))

(def exp-abs
  '(and
    (> x1 2) (> x2 2) (< x1 8) (< x2 8)
    (or (> (+ x2 (* -1 x1)) 4) (> (+ x2 x1) 14))
    (or (> x2 7.5) (< x2 6.5))))
    ; (or (< (+ x2 (* -1 x1)) 0) (< (+ x2 x1) 10))))
    ; (or (< (+ x2 (* -1 x1)) 10) (< (+ x2 (* -1 x1)) 0))))

(def exp-abs
  '(and
    (> x1 2) (> x2 2) (< x1 8) (< x2 8)
    (or (> (+ x2 (* -1 x1)) 4) (> (+ x2 (* 3 x1)) 23))
    (or (> (+ x2 (* -0.1666 x1)) 6.666) (< x2 5))))
    ; (or (< (+ x2 (* -1 x1)) 0) (< (+ x2 x1) 10))))
    ; (or (< (+ x2 (* -1 x1)) 10) (< (+ x2 (* -1 x1)) 0))))

(def exp-testy-3d
  '(and
     (or
       (> (+ x2 (* -1 x1)) 4)
       (> (+ x2 (* 3 x1)) 23)
       (> (+ x1 (* -1 x3)) 0)
       (> (+ x3 (* 1 x2)) 23))
     (or
       (> (+ x1 (* 1 x2) (* 1 x3)) 5)
       (> (+ x2 (* 0.5 x3)) 6)
       (> (+ x1 (* -1 x2) (* -1 x3)) 5)
       (> (+ x3 (* 1 x2)) 0))))

(def exp-rand-3d
  `(~'and
     (~'or
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))
     (~'or
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))))

(def exp-rand-and-3d
  `(~'and
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))))

(def exp-rand-and-3d
  `(~'or
      (~'and
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

        (~'and
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

        (~'and
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

        (~'and
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

            (~'and
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))


        ))
    ; The problem with this is that we'll end up with a ccombinatorial explosion.

; (if (if (> x1 1)
;         true
;         (if (< x2 2)
;              true
;              false))
;     (if (if (> x1 5)
;             true
;             (if (< x2 6)
;                 true
;                 false))
;         true
;         false)
;     false)