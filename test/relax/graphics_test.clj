(ns sigma.graphics-test
  (:use clojure.test
        sigma.graphics
        clozen.helpers))

(def conv-poly
  [[0 0] [5 0] [5 5] [2.5 5] [0 5]])

(deftest convex?-test
  (let [conv-poly [[0 0] [5 0] [5 5] [2.5 5] [0 5]]
        non-conv-poly [[0 0] [5 0] [5 5] [2.5 4.5] [0 5]]
        complex [[1 3] [9 7] [7 9] [7 2] [9 6] [1 8]]]
    (is (= (convex? conv-poly) true))
    (is (= (convex? non-conv-poly) false))))