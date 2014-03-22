; Non-linear planning problem
(defn prior
  "Generate from the prior"
  [n]
  (repeatedly n (fn [] [(rand)(rand)])))

(defn point-in-box?
  "Is a point inside a box?"
  [[[px py] :as point] [[[low-x up-x][low-y up-y]] :as box]]
  (and
    (>= px low-x)
    (<= px up-x)
    (>= py low-y)
    (<= py up-x)))

(defn points-to-vec
  "convert pair of points to vector"
  [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])

(defn path-avoids-obstacles?
  "Does a 2D path avoid (not pass through) any of the obstacles?
   Path is set of vertices, e.g. [[3,3][9,5]]
   Obstacles is set of polys.
   Each poly set of vertices"
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

(defn condition
  [dist pred?]
  (if-let [v (pred? (dist))]
    v
    (recur dist pred?)))

(def obstacles [0 ])

(condition (prior 3) (fn [path] (path-avoids-obstacles? path obstacles)))