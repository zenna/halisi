(ns ^{:doc "Experiments."
      :author "http://clj-me.cgrand.net/2010/09/04/a-in-clojure/"}
  relax.search.a-star
  (:require [clojure.data.priority-map :refer :all]))

(defn A*
 "Finds a path between start and goal inside the graph described by edges
  (a map of edge to distance); estimate is an heuristic for the actual
  distance. Accepts a named option: :monotonic (default to true).
  Returns the path if found or nil."
 [edges estimate start goal & {mono :monotonic :or {mono true}}]
  (let [f (memoize #(estimate % goal)) ; unsure the memoization is worthy
        neighbours (reduce (fn [m [a b]] (assoc m a (conj (m a #{}) b)))
                      {} (keys edges))]
    (loop [q (priority-map start (f start))
           preds {}
           shortest {start 0}
           done #{}]
      (when-let [[x hx] (peek q)]
        (if (= goal x)
          (reverse (take-while identity (iterate preds goal)))
          (let [dx (- hx (f x))
                bn (for [n (remove done (neighbours x))
                         :let [hn (+ dx (edges [x n]) (f n))
                               sn (shortest n Double/POSITIVE_INFINITY)]
                         :when (< hn sn)]
                     [n hn])]
            (recur (into (pop q) bn)
              (into preds (for [[n] bn] [n x]))
              (into shortest bn)
              (if mono (conj done x) done))))))))

(comment
  (require '[relax.search.a-star :refer [A*]]
           '[relax.search.heuristics :refer :all])
  ;; Simple example of shortest path in three node graph
  (def g {'[a b] 1.0 '[b c] 0.5 '[a c] 10.0})
  (A* g zero-heursitic 'a 'c)
  )