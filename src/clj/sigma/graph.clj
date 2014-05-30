(ns ^{:doc "Graph Data Structures"
      :author "Zenna Tavares"}
  sigma.graph)

(defn make-graph
  "An immutable graph data structure
   A node can be any (hashable I suppose) object
   An edge is a map of the form [a b weight]"
  []
  {:nodes #{} :edges []})

(defn nodes
  "A set of nodes of a graph"
  [graph]
  (:nodes graph))

(defn edges
  "A vector of edges"
  [graph]
  (:edges graph))

(defn parent
  "Get the parent node of an edge"
  [edge]
  (first edge))

(defn child
  "Get the child node of an edge"
  [edge]
  (second edge))

(defn weight
  "Get the weight of an edge"
  [edge]
  (nth edge 2))

(defn add-node
  "Add an unattached node to a graph"
  [graph node]
  (update-in graph [:nodes] #(conj % node)))

(defn add-nodes
  "Add unattached nodes to a graph"
  [graph nodes]
  (update-in graph [:nodes] #(concat % nodes)))

(defn add-edge-wo-nodes
  "For performance: Add edge without adding nodes.
   Use only when we can guarantee nodes will already be in there,
   or we don't care for some reason"
   [graph edge]
   (update-in graph [:edges] #(conj % edge)))

(defn add-edges-wo-nodes
  "Add edges"
  [graph edges]
  (update-in graph [:edges] #(concat % edges)))

(defn add-edge
  "Add edge"
  [graph edge]
  ((comp
      #(add-node % (child edge))
      #(add-node % (parent edge))
      #(add-edge-wo-nodes % edge))
    graph))

(defn parents
  "Edge"
  [graph node]
  (set (map parent (filter #(= (child %) node) (edges graph)))))

;; Graphical Model ===================================================================
(defn ancestors
  "Get all the ancestors in a directed graph"
  [graph nodes]
  (loop [ancestors #{} to-visit nodes]
    (if (seq to-visit)
        (let [found-parents
              (set (reduce concat (map #(parents graph %) to-visit)))]
          (recur (set (concat ancestors found-parents)) found-parents))
        ancestors)))

(comment
  (def g (make-graph))
  (def g2 (add-nodes g '[a b c d e]))
  (def g3 (add-edges-wo-nodes g2 ['[b a] '[c a] '[d c]]))
  (ancestors g3 ['c 'a]))

(defn reachable
  [net source observes]
  (let [to-visit z
        ancestors []]
    (loop []
      (let [l (pop )]
        (if (not= ancestors y))

(defn det-separation
  "Return d separation in preence of deterministic cpd"
  [graph d x y z])

(comment
  (require '[sigma.graph :refer :all])
  (-> (make-graph) (add-node 'a) (add-node 'c) (add-edge ['d 'a 10]))
  ;; => {:nodes #{a c d}, :edges [[d a 10]]}
  )
