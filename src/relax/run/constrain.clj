(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.run.constrain
  (:require [relax.constrain :refer :all]
            [relax.examples :refer :all]
            [relax.domains.box :refer :all]
            [relax.common :refer :all]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]
            [clozen.profile.bucket :refer :all]))

; (defn -main[]
;   (let [{vars :vars pred :pred}
;         (avoid-orthotope-obs 3
;                             [1 1n] [9 9] 
;                             [[[2 4][4 6]]]
;                             0.5)
;         ; (qual-example)
;         vars (vec vars)]
;   (samples-to-file
;     "path_samples2"
;     (:result (first
;       (bucket-test-force [] {:remove-inconsistent? 1}
;         (profile :info :take-samples (take-samples pred vars 10))))))))

(def four-tiled-obstacles (mapv :internals (tile-boxes 2 4)))

(defn -main[]
  (let [{vars :vars pred :pred}
        (point-avoid-orthotope-obs 1 four-tiled-obstacles)]
    (construct-box-domain vars pred)))

; (defn -main[]
;   (let [{vars :vars pred :pred}
;         (avoid-orthotope-obs 4
;                             [1 1] [7 6] 
                            ; [[[3 5][0 3]]
                            ;  [[3 5][3.5 9]]
                            ;  [[5.5 7][2 4]]]
                            ; 1.4)
;         ; (qual-example)
;         vars (vec vars)]
;   (samples-to-file
;     "path_samples2"
;     (:result (first
;       (bucket-test-force [] {:remove-inconsistent? 1}
;         (profile :info :take-samples (take-samples pred vars 10))))))))