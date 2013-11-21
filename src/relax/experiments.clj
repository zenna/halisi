(ns ^{:doc "Experiments."
      :author "Zenna Tavares"}
  relax.experiments
  (:require [relax.constrain :refer :all]
            [relax.examples :refer :all]
            [clozen.helpers :refer :all]
            [clozen.profile.scaling :refer :all]
            [clozen.profile.bucket :refer :all]
            [clozen.profile.plot :refer :all]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]))

; The scientific questions I want to answer are:
; 1. What is the effect of 
; . a) doing the reduction, the pruning
;   b) doing the inconsistency checking
;   c) doing the joining

; On run-time and rejected samples.
; For rejected samples I just need a bar chart

; How to interface the prior and the predicate
; TODO: Need to fix scaling so that it works with n-samples = 1
; TODO: Disable printing, use loggin
; TODO: Write tests to see if samplers are actually working

(defn plan-by-rejection
  "Path planning example with rejection sampler"
  [n-samples]
  (let [{vars :vars pred :pred}
          (avoid-orthotope-obs 3
                               [1 1] [9 9] 
                               [[[3 6][0 3.5]]
                                [[0 2][5 7]]
                                [[4 7][5 7]]]
                               10)
          vars (vec vars)
          var-intervals (zipmap vars (repeat (count vars) [0 10]))

          ; Change sample of first and last region
          var-intervals (assoc var-intervals 'x0 [0.9 1.1])
          var-intervals (assoc var-intervals 'y0 [0.9 1.1])
          var-intervals (assoc var-intervals 'y9 [8.9 9.1])
          var-intervals (assoc var-intervals 'y9 [8.9 9.1])

          prior (make-uniform-prior vars var-intervals)
          sampler (naive-rejection vars pred prior)]
    (p :sampling-time (doall (repeatedly n-samples sampler)))))

(defn plan-by-construct
  "This experiment tests how methods scale
   Returns a function of the number of samples" 
  [n-samples]
  (let [{vars :vars pred :pred}
        (avoid-orthotope-obs 3
                             [1 1] [9 9] 
                             [[[3 6][0 3.5]]
                              [[0 2][5 7]]
                              [[4 7][5 7]]]
                             10)
        vars (vec vars)
        sampler (constrain-uniform-divisive vars pred)]
  (p :sampling-time (doall (repeatedly n-samples sampler)))))

(defn run-all-experiments
  ""
  []
  (coll-to-file
    (bucket-scaling-plot
      (bucket-test
        [:sample-type]
        (scaling (bucket :sample-type plan-by-construct plan-by-rejection)
                 identity
                 (map vector (range 10 800 120)) 2))
       [:taoensso.timbre.profiling/whole :max]
       [:taoensso.timbre.profiling/sampling-time :max])
    "zennabadman"))

