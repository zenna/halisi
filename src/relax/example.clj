(ns ^{:doc "Example generative models and constraints"
	    :author "Zenna Tavares"}
  relax.example
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
(defn test-rejection-acyclic
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

(defn avoids-obstacles?
  [path])

;; Inverse Graphics
(defn gen-poly [])

(defn simple? [])

(defn gen-mesh [])

(defn self-intersections? [])