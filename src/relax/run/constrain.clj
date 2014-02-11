(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.run.constrain
  (:require [relax.constrain :refer :all]
            [relax.examples.planning :refer :all]
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

(def poly-obstacle-a [[0.0 0.0][7.0 3.0][1.0 5.0]])
(def poly-obstacle-b [[7 8][10 8][10 9][7 9]])
(def four-bricks
  [[[1.0 1.0][4.0 1.0][4.0 4.5][1.0 4.5]]
   [[5.0 1.0][9.0 1.0][9.0 4.5][5.0 4.5]]
   [[5.0 6.0][9.0 6.0][9.0 9.5][5.0 9.5]]
   [[1.0 6.0][4.0 6.0][4.0 9.5][1.0 9.5]]])

(defn -main[]
  (let [{vars :vars pred :pred}
        ; (point-avoid-orthotope-obs 1 four-tiled-obstacles)
        (lambda-points-avoid-poly-obs 1 four-bricks)]
    (samples-to-file
      "samples.xscatter"
      (take-samples pred vars 2000))))

; (defn -main[]
;   (let [{vars :vars pred :pred}
;         (avoid-orthotope-obs 4
;                             [1 1] [7 6] 
;                             [[[3 5][0 3]]
;                              [[3 5][3.5 9]]
;                              [[5.5 7][2 4]]]
;                             1.4)
;         ; (qual-example)
;         vars (vec vars)]
;   (samples-to-file
;     "path_samples2"
;     (:result (first
;       (bucket-test-force [] {:remove-inconsistent? 1}
;         (profile :info :take-samples (take-samples pred vars 10))))))))