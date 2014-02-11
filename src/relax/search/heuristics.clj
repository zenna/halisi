(ns ^{:doc "Heuristics for use with search algorithms."
      :author "Zenna Tavares"}
  relax.search.heuristics)

(defn euclidean-distance [a b]
  "Multidimensional euclidean-distance"
  (Math/sqrt (reduce + (map #(let [c (- %1 %2)] (* c c)) a b))))

(defn zero-heuristic [a b]
  "Uniformative heuristic.  Use when no reasonable admissible heuristic can be found.
   Will reduce A* to Dijikstra's algorithm."
  0.0)

(defn grid [x y w h]
  "Generate a grid graph whose outlying edges are one-way.
   Returns graph as map of the form {[src target] weight .. }"
  (into {}
    (for [i (range w) j (range h)
          :let [x0 (+ x i) y0 (+ y j) x1 (inc x0) y1 (inc y0)]]
      {[[x0 y0] [x1 y0]] 1
       [[x1 y0] [x1 y1]] 1
       [[x1 y1] [x0 y1]] 1
       [[x0 y1] [x0 y0]] 1})))

(comment
  (require '[relax.search.a-star :refer [A*]])
  (def g (apply dissoc (grid 0 0 4 4) (keys (grid 1 1 2 2))))
  (A* g euclidian-distance [0 3] [4 2]))