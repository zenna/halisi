(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.run.constrain
  (:require [relax.constrain :refer :all])
  (:require [relax.examples :refer :all])
  (:require [taoensso.timbre.profiling :as profiling :refer (p o profile)])
  (:require [relax.domains.box :refer :all]))

(defn -main[]
  (let [{vars :vars pred :pred}
        (avoid-orthotope-obs 3
                            [1 1] [9 9] 
                            [[[3 6][0 3.5]]
                             [[0 2][5 7]]
                             [[4 7][5 7]]]
                            10)
        vars (vec vars)]
  (profile :info :whatevs (p :FYLL (take-samples pred vars 100)))))

; (defn -main[]
;   (let [{vars :vars pred :pred}
;         (avoid-orthotope-obs 7
;                             [1 1] [9 9] 
;                             [[[3 6][0 3.5]]
;                              [[0 2][5 7]]
;                              [[4 7][5 7]]]
;                             10)
;         vars (vec vars)]
;   (profile :info :whatevs (p :FYLL (take-samples pred vars 100)))))

; (defn -main[]
;   (let [{vars :vars pred :pred}
;         (avoid-orthotope-obs 3
;                             [1 1] [9 9] 
;                             [[[1.5 4.5][3 7]]]
;                             10)
;         vars (vec vars)]
;   (profile :info :whatevs (p :FYLL (take-samples pred vars 100)))))

; (def pred-x
;   '(and
;      a
;      b
     

;      (or c d e f)
;      (or g h i j)))

; (def exp1
;   '(or (and
;         (or a b)
;         c
;         (and e f))
;         g))

; (defn -main[]
;   (let [dnf (to-dnf-new '[a b c d e f g h i j x] nil
;               (join-substitute pred-x ['[c d e f]]))]
;     (println "count" (count dnf) "\n" dnf)))

; (defn -main[]
;   (take-samples exp-linear '[x1 x2] 100))