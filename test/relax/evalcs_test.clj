(ns relax.evalcs-test
  (:use clojure.test
        relax.evalcs
        relax.symbolic
        clozen.helpers))

(deftest evalcs-test
  (let []
    (is (= (evalcs '(+ 1 (* 2 3)) the-global-environment) 7))))
  
(deftest negate-test
  (is (= (negate '(> x 3)) '(<= x 3)))
  (is (= (negate '(< y 2)) '(>= y 2)))
  (is (= (negate '(>= z 3)) '(< z 3)))
  (is (= (negate '(<= p 0)) '(> p 0)))
  (is (= (negate '(arb-pred? a1 a2)) '(not (arb-pred? a1 a2)))))

; (deftest unroll-and-ors-test
;   (let [exp
;           '(if (> x1 0.5)
;               (and (> x2 0.7) (< x2 0.9))
;               (and (> x1 0.1) (< x1 0.4)
;                    (> x2 0.3) (< x2 0.5)
;                    (or (< x3 0.1) (> x3 0.9))))]
;   (is (= (unroll-and-ors-test exp)))))

; (deftest eval-concrete-test
;   ())
; (if (= 2 (- 3 1)) (* 2 3) (+ 3 4))


; (def exp '(if (> x1 0.5)
;     (and (> x2 0.7) (< x2 0.9))
;     (and (> x1 0.1) (< x1 0.4)
;          (> x2 0.3) (< x2 0.5)
;          (or (< x3 0.1) (> x3 0.9)))))

; (if (> x1 (+ 3 2))
;   (if (> x2 0.7)
;     (if (< x2 0.9)
;         true
;         false)
;     false)
;   (if (> x1 0.1)
;       (if (< x1 0.4)
;           (if (> x2 0.3)
;               (if (< x2 0.5) 
;                   (if (if (< x3 0.1)
;                           true
;                           (if (> x3 0.9)
;                               true
;                               false))
;                       true
;                       false)
;                   false)
;               false)
;           false)
;       false))

; (if (> x1 (+ 3 2))
;   (if (> x2 0.7)
;     (if (< x2 0.9)
;         true
;         false)
;     false)
;   false)

; (if (> x1 (+ 3 2))
;   (if (> x2 0.7)
;       true
;       false)
;   true)
