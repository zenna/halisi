(ns ^{:doc "Rapidly Exploring Random Trees."
      :author "Zenna Tavares"}
  relax.search.rrt
  (:require [clozen.helpers :as clzn :refer [flip]])
  (:require [relax.graph :refer :all]
            [relax.geometry :refer :all])
  (:require [clojure.data.priority-map :refer :all]))

(defn nearest-node
  "Find the nearest node of graph g to point in configuration space.
   Linear search.

   Ideally we would connect the newly sampled point to the closest point
   in graph, if this point is on an edge, the edge should be split.
   This is an approximation of that ideal."
  [point g distance]
  (loop [nodes (nodes g) closest-node (first nodes)
         closest-distance Double/POSITIVE_INFINITY]
    (if (seq nodes)
        (let [node (first nodes)
              dist (distance point (first nodes))]
          (if (< dist closest-distance)
              (recur (next nodes) node dist)
              (recur (next nodes) closest-node closest-distance)))
        closest-node)))

(defn stopping-config
  "Yields the nearest configuration possible to the boundary of C_free,
   along the direction form q_a to t_b.

   Obstacles is a collection of line segments (each represented as pair of vertices)
   [[a b][c d][e f]..]"
  [q-a q-b obstacles]
  (loop [smallest-s 1.0 obs (reduce concat (mapv poly-to-edges obstacles))]
    (if (seq obs)
        (let [s1 (intersection-point [q-a q-b] (first obs))
              s2 (intersection-point (first obs) [q-a q-b])

              ;; If BOTH are between zero and one, they intersect, then return s
              s (if (and (>= s1 0.0) (<= s1 1.0) (>= s2 0.0) (<= s2 1.0))
                     (* s1 0.9) ;; Reduce by factor to avoid points landing ON obst. line
                     1.0)]
          (if (< s smallest-s)
              (recur s (next obs))
              (recur smallest-s (next obs))))
      smallest-s)))

(defn make-rdt
 "Build a RDT - Rapidly Exploring Dense Tree (i.e. a road map graph).
  Assiumes graph nodes are coordinates in R, e.g. [a b]."
 [q-start q-dest max-nodes delta-q sampler distance obstacles]
 (loop [graph (add-node (make-graph) q-start) n-nodes-left (dec max-nodes)
        found-dest false]
  (let [q-rand (if (or found-dest (clzn/flip 0.99)) ; Most of the time:
                   (sampler)            ;  Sample a new point
                    q-dest)             ; Occasionally: Attempt to connect to goal config
        q-near (nearest-node q-rand graph distance)
        stop-s (stopping-config q-near q-rand obstacles)
        q-new (parametric-to-point q-near q-rand stop-s)
        new-graph (if (> (distance q-near q-new) delta-q)
                      (add-edge graph [q-near q-new 1.0])
                      graph)]
    (when (= q-dest q-new)
          (println "Reached target in iteration: " (- max-nodes n-nodes-left)))
    (cond
      (= q-dest q-new)
      (- max-nodes n-nodes-left)

      (zero? n-nodes-left)
      new-graph ; TODO, interpolate.
      
      :else
      (recur new-graph (dec n-nodes-left)
                       (if found-dest true (= q-dest q-new)))))))

(comment
  (require '[relax.search.rrt :refer :all]
           '[relax.box :refer [interval-sample]]
           '[relax.search.heuristics :refer [euclidean-distance]])
  (def q-start [0.5 0.5])
  (def q-dest [9.9 9.9])
  (def poly-obstacle-a [[1.0 1.0][7.0 3.0][1.0 5.0]])
  (def poly-obstacle-b [[7 8][10 8][10 9][7 9]])

  (def four-bricks
    [[[1.0 1.0][4.0 1.0][4.0 4.5][1.0 4.5]]
     [[4.1 1.0][9.0 1.0][9.0 4.5][4.1 4.5]]
     [[4.1 6.0][9.0 6.0][9.0 9.5][4.1 9.5]]
     [[1.0 6.0][4.0 6.0][4.0 9.5][1.0 9.5]]])

  (def max-nodes 500)
  (def delta-q 0.5)
  ; Samples a point in 10 x 10 square.
  (defn sampler [] (vec (interval-sample [[0 10] [0 10]])))

  (require '[relax.constrain :refer [constrain-uniform-divisive]]
           '[relax.examples.planning :refer [lambda-points-avoid-poly-obs]])
  (def vars-pred (lambda-points-avoid-poly-obs 1 four-bricks))
  (def smart-sampler (constrain-uniform-divisive (:vars vars-pred)
                                                  (:pred vars-pred)))
  (defn bright-sampler [] (-> (smart-sampler) :sample vec))
  (bright-sampler)
  ; Constrain the sampler to avoid points
  ; (def smart-sampler
  ;   (construct sampler (fn [sample] (not-any? #(intersect sample %))
  ;                                    obstacles)))
  (def graph (make-rdt q-start q-dest max-nodes delta-q sampler euclidean-distance four-bricks))
  (coll-to-file (edges graph) "rrt-output2"))