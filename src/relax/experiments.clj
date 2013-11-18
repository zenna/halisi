(ns ^{:doc "Experiments."
      :author "Zenna Tavares"}
  relax.experiments
  (:require [relax.constrain :refer :all])
  (:require [relax.examples :refer :all])
  (:require [clozen.helpers :refer :all])
  (:require [clozen.profile :refer :all])
  (:require [taoensso.timbre.profiling :as profiling :refer (p o profile)]))

; How to interface the prior and the predicate
; Prior returns list. Interpreter applies predicate to list
; if we just do function application then all our predicates will be unary.
; Go with apply

; TODO: Need to fix scaling so that it works with n-samples = 1
; TODO: Need to make more efficient so I can just pipe into python script
; TODO: Need easy way to compare with rejection sampling
; TODO: Need easy way to compare wit h different versions

; ; 1. Comparison with rejection

; Goal: (compare orthotope-experiment
;                rejection-sampling
;                what-sampling)

; This is at least sillyly named because our rejection sampler
; is in orthotope sampler.

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
          sampler (naive-rejection2 vars pred prior)]
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
                         [[100] [200] [300] [400] [500]] 2)
        construct-run-times
        (scale-indep-input construct-results
          [:taoensso.timbre.profiling/whole :max]
          [:taoensso.timbre.profiling/sampling-time :max])
        reject-results (scaling plan-by-rejection identity
                       [[10] [20] [30] [40] [50]] 2)
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

; Problems
; 1. I have a lot of duplication
; 2. I have two samples

; (def a [{[10] [[295133622 150166917]], [20] [[145979261 127165609]], [30] [[115351336 134856436]], [40] [[124280018 114019402]], [50] [[102789291 140268403]]}])

; In this case it's 100 times slower
; There are many possible reasons for this.
;; 1. The compilation time is taking up too much of the run time
;; - This would be become clear as the number of samples increases heavily
;; - Or separating out compilation time from whole time

;; 2. The printing to screen is making a big difference
;; - This would become clear by removing the printing to screen

;; 3. My rejection sampler isn't actually working
;; - Should check this

