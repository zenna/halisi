(ns relax.evalcs-test
  (:use clojure.test
        relax.evalcs
        relax.env
        relax.symbolic
        relax.constrain
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

(deftest add-condition-test
  (is (= (add-condition true '(> x 3))
     '(conditioned-value true [(> x 3)])))
  (is (= (add-condition (add-condition false '(> x 3))
                         '(< y 4))
     '(conditioned-value false [(> x 3) (< y 4)]))))

(deftest revise-interval-test
  (is (= (revise-interval [0 10] '> 5) [5 10]))
  (is (= (revise-interval [0 10] '<= 5) [0 5]))
  (is (= (revise-interval [0 10] '< 13) [0 10]))
  (is (= (revise-interval [0 10] '> -5) [0 10])))

(deftest handle-condition-test
  (let [x1 (make-conditional-value 3 'godisgood 4 'notsogood)
        x2 (make-conditional-value 10 'satanisbad 100 'ilikethefire)
        result '(conditional-value (multivalue [(possible-value 14 [godisgood satanisbad]) (possible-value 104 [godisgood ilikethefire]) (possible-value 15 [notsogood satanisbad]) (possible-value 105 [notsogood ilikethefire])]))]
    (is (= (handle-condition-test + 1 2 x1 x2))

(deftest evalcs-test
  (is (= (evalcs '(if true 2 3) the-global-environment) 2))
  (define-variable! 'x (make-multivalue 1 2 3) the-global-environment)
  (is (= 2 (evalcs '(if (> x 2) true false) the-global-environment))))