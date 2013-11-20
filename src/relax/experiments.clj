(ns ^{:doc "Experiments."
      :author "Zenna Tavares"}
  relax.experiments
  (:require [relax.constrain :refer :all])
  (:require [relax.examples :refer :all])
  (:require [clozen.helpers :refer :all])
  (:require [clozen.profile.scaling :refer :all])
  (:require [taoensso.timbre.profiling :as profiling :refer (p o profile)]))

; The scientific questions I want to answer are:
; 1. What is the effect of 
; . a) doing the reduction, the pruning
;   b) doing the inconsistency checking
;   c) doing the joining

; On run-time and rejected samples.
; For rejected samples I just need a bar chart

; How to interface the prior and the predicate
; TODO: Need to fix scaling so that it works with n-samples = 1
; TODO: Need to make more efficient so I can just pipe into python script
; TODO: Need easy way to compare wit h different versions
; TODO: Disable printing, use loggin
; TODO: Write tests to see if samplers are actually working
; How to enable/disable different parts for testing
; -- Simplest solution. Just rename different functions

; (defn my-test-function
;   (let [x (bucket (my-first-function 10 20 30)
;                   (my-second-function 10 20 30))]

; I am looking for a way to differentially test different version of the code
; One example is in using different versions

; The problem with interopabiltiy with scaling is that there's no way for
; scaling as a function to know that there are two buckets to test
; And what if you have multiple buckets.

; There's a more elegant way to construct this I am sure, but for now I'll just
; do it manually.
; What to test
;; Impact of 

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
  "Run all the experiments, silly"
  []
  (let [
        construct-results (scaling plan-by-construct identity
                         (map vector (range 10 800 60)) 2)
        construct-run-times
        (scale-indep-input construct-results
          [:taoensso.timbre.profiling/whole :max]
          [:taoensso.timbre.profiling/sampling-time :max])
        reject-results (scaling plan-by-rejection identity
                       (map vector (range 10 800 60)) 2)
        reject-run-times
        (scale-indep-input reject-results
          [:taoensso.timbre.profiling/whole :max]
          [:taoensso.timbre.profiling/sampling-time :max])
        ]
    [reject-results
    ;  construct-results
     construct-run-times
    (flatten-scaling-data construct-run-times)
    (flatten-scaling-data reject-run-times)]))

