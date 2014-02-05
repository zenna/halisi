(ns ^{:doc "Graph Data Structures"
      :author "Zenna Tavares"}
  relax.graph)

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

(defn add-edge-wo-nodes
  "For performance: Add edge without adding nodes.
   Use only when we can guarantee nodes will already be in there,
   or we don't care for some reason"
   [graph edge]
   (update-in graph [:edges] #(conj % edge)))

(defn add-edge
  "Add an unattached node to a graph"
  [graph edge]
  ((comp
      #(add-node % (child edge))
      #(add-node % (parent edge))
      #(add-edge-wo-nodes % edge))
    graph))

(comment
  (require '[relax.graph :refer :all])
  (-> (make-graph) (add-node 'a) (add-node 'c) (add-edge ['d 'a 10]))
  ;; => {:nodes #{a c d}, :edges [[d a 10]]}
  )