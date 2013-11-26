(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.run.constrain
  (:require [relax.constrain :refer :all]
            [relax.examples :refer :all]
            [relax.domains.box :refer :all]
            [relax.common :refer :all]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]
            [clozen.profile.bucket :refer :all]))

(defn -main[]
  (let [{vars :vars pred :pred}
        (avoid-orthotope-obs 8
                            [1 1] [9 8] 
                            [[[3 7][0 3]]
                             [[3 7][3.5 9]]]
                            1.5)
        vars (vec vars)]
  (samples-to-file
    "planning_samples"
    (:result (first
      (bucket-test-force [] {:remove-inconsistent? 1}
        (profile :info :take-samples (take-samples pred vars 20))))))))