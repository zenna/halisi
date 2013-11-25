(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.run.constrain
  (:require [relax.constrain :refer :all]
            [relax.examples :refer :all]
            [relax.domains.box :refer :all]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]
            [clozen.profile.bucket :refer :all]))

(defn -main[]
  (let [{vars :vars pred :pred}
        (avoid-orthotope-obs 5
                            [1 1] [9 9] 
                            [[[3 6][0 3.5]]
                             [[0 2][5 7]]
                             [[4 7][5 7]]]
                            10)
        vars (vec vars)]
  (bucket-test []
    (profile :info :whatevs (p :FYLL (take-samples pred vars 100))))))