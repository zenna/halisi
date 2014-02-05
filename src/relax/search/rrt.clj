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
    (cond
      (seq nodes)
      closest-node

      :else
      (let [node (first nodes)
            dist (distance point (first nodes))]
        (if (< dist closest-distance)
            (recur (next nodes) node dist)
            (recur (next nodes) closest-node closest-distance))))))

(defn stopping-config
  "Yields the nearest configuration possible to the boundary of C_free,
   along the direction form q_a to t_b.

   Obstacles is a collection of line segments (each represented as pair of vertices)
   [[a b][c d][e f]..]

   Returns

   WHATIF parallel
   AM I on the right side

   "
   [q-a q-b obstacles]
   (loop [smallest-s 1.0 o obstacles]
    (if (seq obstacles) smallest-s
        (let [s (intersection-point [q-a q-b] (first o))
              s (if (or (> s 1.0) (< s 1.0) (= :parallel s))
                    1.0
                    s)]
        (if (< s smallest-s)
            (recur s (next obstacles))
            (recur smallest-s (next obstacles)))))))

(defn make-rdt
 "Build a RDT - Rapidly Exploring Dense Tree (i.e. a road map graph).
  Assiumes graph nodes are coordinates in R, e.g. [a b]"
 [q-start q-dest max-nodes delta-q sampler distance obstacles]
 (loop [graph (add-node (make-graph) q-start) n-nodes-left (dec max-nodes)]
  (let [q-rand (if (clzn/flip 0.99)     ; Most of the time:
                   (sampler)            ;  Sample a new point
                    q-dest)             ; Occasionally: Attempt to connect to goal config
        q-near (nearest-node q-rand graph distance)
        q-new (parametric-to-point
                q-near q-rand
                (stopping-config q-near q-rand obstacles))]
    (when (= q-dest q-new)
          (println "Got target in iteration: " (- max-nodes n-nodes-left)))
    (if (zero? n-nodes-left)
        (add-edge graph [q-near q-new 1.0]) ; TODO, interpolate.
        (recur (add-edge graph [q-near q-new 1.0]) (dec n-nodes-left))))))

(comment
  (require '[relax.search.rrt :refer :all]
           '[relax.box :refer [interval-sample]]
           '[relax.search.heuristics :refer [euclidean-distance]])
  (def q-start [1.0 1.0])
  (def obstacles [[[3.1 4.1][3.0 7.0]]
                  [[3.0 7.0][6.1 7.05]]
                  [[6.1 7.05]][[6.11 2.05]]])
  (def q-dest [1.0 1.0])
  (def max-nodes 50)
  (def delta-q 1.0)
  (defn sampler [] (vec (interval-sample [[0 10] [0 10]]))) ; Samples a point in 10 x 10 square.
  (def graph (make-rdt q-start q-dest max-nodes delta-q sampler euclidean-distance obstacles)))



