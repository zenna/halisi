(ns ^{:doc "Examples in 2D, 3D and ND planning"
	    :author "Zenna Tavares"}
  relax.examples.planning
  (:require [relax.geometry.common :refer :all]
            [relax.geometry.convex :refer [convex-hull-gf]]
            [relax.geometry.svg :refer :all])
  (:require [relax.domains.box :refer [middle-split]])
  (:require [clozen.helpers :refer :all]))

(defn parse-scene-data
  "Take an svg file and get out the obstacles, boundary and start and dest.
   svg must be in absolute coordinates.
   objects should be id'd 'dest' 'start' 'boundary', and othe ids will be
   considered obstacles."
  [svg-fname]
  (let [svg-scene (svg-file-to-poly svg-fname)
        boundary (:data (first (filterv #(= "boundary" (:id %)) svg-scene)))
        q-start-reg (:data (first (filterv #(= "start" (:id %)) svg-scene)))
        q-dest-reg (:data (first (filterv #(= "dest" (:id %)) svg-scene)))
        q-start (vec (middle-split {:internals q-start-reg}))
        q-dest (vec (middle-split {:internals q-dest-reg}))
        obstacles
          (mapv convex-hull-gf
            (extract (filterv #(= :path (:type %)) svg-scene) :data))
        obstacles (mapv #(if (clockwise? %) (vec (reverse %)) %) obstacles)]
    {:boundary boundary :obstacles obstacles
     :start q-start :dest q-dest
     :start-region q-start-reg :dest-region q-dest-reg}))

(defn gen-path-parametric
  "Generate a random path n-dim dimensions"
  [n-dim num-points]
  (repeatedly num-points #(vec (repeatedly n-dim rand))))

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

(defn lambda-valid-path-linear
  [n-points [sx sy :as start] [ex ey :as end] obstacles max-step]
  "Creates a program which when evaluated on a path will return true
   only if that path passes through no obstacles
   obstacles is a vector of points = [[x1-min x1-max][x2-min x2-max]]"
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
    ~(:pred (lambda-points-avoid-poly-obs n-points obstacles)))}))

(defn path-avoids-obstacles?
  "Does a 2D path avoid (not pass through) any of the obstacles?

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
  "Path is valid if start is in start box, dest is in dest box
   and it avoids obstacles"
  [start target obstacles path]
  (and
    (point-in-box? (first path) start)
    (point-in-box? (last path) target)
    (path-avoids-obstacles? obstacles path)))

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