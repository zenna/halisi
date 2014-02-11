(ns ^{:doc "Examples in 2D, 3D and ND planning"
	    :author "Zenna Tavares"}
  relax.examples.planning
  (:require [relax.geometry :refer :all])
  (:require [clozen.helpers :refer :all]))

(defn gen-path-parametric
  "Generate a random path in X Y"
  [num-dim num-points]
  (repeatedly num-points #(vec (repeatedly num-dim rand))))

; (defn avoids-obstacles?
;   "When evaluated on a path will return true
;    only if that path passes through no obstacles
;    obstacles is a vector of points = [[x1-min x1-max][x2-min x2-max]]"
;   [path start target obstacles max-step]
;   (let [p-start (first path)
;         p-end (last path)]

;     (and
;       (in-box? p-start start)     ; First point must be in start
;       (in-box? p-end target)      ; Last point must be in target

;                                   ; Points must be at most max-distance apart
;       (apply and
;         (for [p path]
;           (in-box? p (square-around-point point step))))

;                                   ; Points must not be within obstacles
;       (apply or
;         (for [p path              ; Consider all combinations of points
;               o obstacles]        ;   and obstacles
;           (not (in-box? p o)))))))   ; Check point is not in obstacle 

;; Linear Planning Problem
(defn avoid-orthotope-obs
  "Creates a program which when evaluated on a path will return true
   only if that path passes through no obstacles
   obstacles is a vector of points = [[x1-min x1-max][x2-min x2-max]]"
  [n-points [sx sy :as start] [ex ey :as end] obstacles max-step]
  (let [pos-delta 0.1
        vars
        (for [i (range n-points)]
          [(symbol (str "x" i)) (symbol (str "y" i))])
        [svx svy] (first vars)
        [evx evy] (last vars)]
    {:vars (vec (reduce concat vars))
     :pred
  `(~'and

    ; First point must be in start box
    (~'>= ~svx ~(- sx pos-delta))
    (~'<= ~svx ~(+ sx pos-delta))
    (~'>= ~svy ~(- sy pos-delta))
    (~'<= ~svy ~(+ sy pos-delta))

    ; Last point must be in target box
    (~'>= ~evx ~(- ex pos-delta))
    (~'<= ~evx ~(+ ex pos-delta))
    (~'>= ~evy ~(- ey pos-delta))
    (~'<= ~evy ~(+ ey pos-delta))

    ; Points must be certain distane apart
    ~@(reduce concat
        (for [[[path-x0 path-y0] [path-x1 path-y1]] (partition 2 1 vars)]
          `[(~'>= (~'+ ~path-x1 (~'* -1 ~path-x0)) 0)
            (~'<= (~'+ ~path-x1 (~'* -1 ~path-x0)) ~max-step)
            (~'>= (~'+ ~path-y1 (~'* -1 ~path-y0)) 0)
            (~'<= (~'+ ~path-y1 (~'* -1 ~path-y0)) ~max-step)]))

    ; Points must not be within obstacles
    ~@(for [[x y] (subvec (vec vars) 1 (dec (count vars)))
              [[x-min x-max][y-min y-max]] obstacles]
          `(~'or
            (~'<= ~x ~x-min)
            (~'>= ~x ~x-max)
            (~'<= ~y ~y-min)
            (~'>= ~y ~y-max))))}))

(defn point-avoid-orthotope-obs
  "Subset of constraints of avoid-orthotope-obs: ensures none of a
   set of points fall within rectangular obstacles"
  [n-points obstacles]
  (let [vars
        (for [i (range n-points)]
          [(symbol (str "x" i)) (symbol (str "y" i))])]
    {:vars (vec (reduce concat vars))
     :pred
  `(~'and

    ; Points must not be within obstacles
    ~@(for [[x y] vars
            [[x-min x-max][y-min y-max]] obstacles]
          `(~'or
            (~'<= ~x ~x-min)
            (~'>= ~x ~x-max)
            (~'<= ~y ~y-min)
            (~'>= ~y ~y-max))))}))

(defn lambda-points-avoid-poly-obs
  "Make a predicate expression testing:
   does a point not fall inside any (i.e. avoid all the) obstacles?"
  [n-points obstacles]
  (let [h-forms (map v-form-to-h obstacles)
        pvar (println "H-forms are" h-forms)
        vars
        (for [i (range n-points)]
          [(symbol (str "x" i)) (symbol (str "y" i))])]
    {:vars (vec (reduce concat vars))
     :pred
  `(~'and

    ; Points must be certain distane apart
    ~@(for [[x y] (partition 2 (reduce concat vars))
              obstacle h-forms]
        `(~'or
           ~@(for [[a0 a1 b] obstacle]
              ; switch inequality from < to > to emulate not
              ; i.e. to not of a comvex polytope we OR all ineqs
              ; invert each inequality 
              `(~'> (~'+ (~'* ~a0 ~y) (~'* ~a1 ~x)) ~b)))))}))

;; Non-linear planning constraint
; (defn intersection-point
;   "Find the intersection point of two vectors"
;   [[a0 a1] [b0 b1]]
;   (let [[u1 u2] (points-to-vec a1 a0)
;         [v1 v2] (points-to-vec b0 b1)
;         [w1 w2] (points-to-vec b0 a1)]
;     (/ (- (* v2 w1) (* v1 w2))
;        (- (* v1 u2) (* v2 u1)))))

(defn path-avoids-obstacles?
  "Does the path avoid (not pass through) any of the obstacles?

   Path is set of vertices, e.g. [[3,3][9,5]]
   Obstacles is set of edges [e1,..,en]
   where ei is pair of vertices.
   e.g. obstacles = 
   [[[3 9][7 9]]
    [[7 9][5 7]]
    [[5 7][3 9]]]"
  [obstacles path]
  (every?
    (for [[p0 p1] (partition 2 1 path)
          [o0 o1] obstacles
           :let [[u1 u2] (points-to-vec p1 p0)
                 [v1 v2] (points-to-vec o0 o1)
                 [w1 w2] (points-to-vec o0 p1)
                 s (/ (- (* v2 w1) (* v1 w2))
                      (- (* v1 u2) (* v2 u1)))]]
      (or (> s 1.0)
          (< s 1.0)))))

(defn valid-path?
  "Path is valid if start is in start, dest is in dest and
   it avoids obstacles"
  [start target obstacles path]
  (and
    (point-in-box? (first path) start)
    (point-in-box? (last path) target)
    (path-avoids-obstacles? obstacles path)))

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

(comment
  (require '[relax.examples.planning :refer :all])
  (def poly-obstacle [[0.0 0.0][1.0 5.0][7.0 3.0]])
  (lambda-points-avoid-poly-obs 1 [poly-obstacle])

  (def obstacle-eg
    [[[3 9][7 9]]
     [[7 9][5 7]]
     [[5 7][3 9]]])

  (def path-eg
    [[3,3][9,5]]))