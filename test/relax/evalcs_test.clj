(ns relax.evalcs-test
  (:use clojure.test
        relax.evalcs
        clozen.helpers))

(deftest evalcs-test
  (let []
    (is (= (evalcs '(+ 1 (* 2 3)) the-global-environment) 7))))

(deftest unroll-and-ors-test
  (let [exp
          '(if (> x1 0.5)
              (and (> x2 0.7) (< x2 0.9))
              (and (> x1 0.1) (< x1 0.4)
                   (> x2 0.3) (< x2 0.5)
                   (or (< x3 0.1) (> x3 0.9))))]
  (is (= (unroll-and-ors-test exp))