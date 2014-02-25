(ns ^{:doc "Experiments."
      :author "Zenna Tavares"}
  relax.experiments
  (:require [relax.constrain :refer :all]
            [relax.examples.planning :refer :all]
            [relax.common :refer [samples-to-file]])
  (:require [clozen.helpers :refer :all]
            [clozen.profile.scaling :refer :all]
            [clozen.profile.bucket :refer :all]
            [clozen.profile.plot :refer :all])
  (:require [taoensso.timbre.profiling :as profiling :refer (p o profile)]))

(defn plan-using-constrain []
  "A path planning experiment with polygonal obstacles"
  (let [n-points 3
        plan-scene (parse-scene-data "plan_star2.svg")
        {vars :vars pred :pred}
        (lambda-valid-path-linear n-points (:start plan-scene) (:dest plan-scene) (:obstacles plan-scene) 3.0)
        prior (vec (reduce concat (repeat n-points (:boundary plan-scene))))]
    (println "prior" prior "vars" vars)
    (samples-to-file
      "samples.xscatter17"
      (take-samples vars prior pred 2))))

(defn plan-by-rejection
  "Path planning example with rejection sampler"
  [constraint n-samples]
  (let [{vars :vars pred :pred} constraint
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
  [constraint n-samples]
  (let [{vars :vars pred :pred} constraint
        vars (vec vars)
        sampler (construct vars pred)]
  (p :sampling-time (doall (repeatedly n-samples sampler)))))

  (defn map-to-python-dict
    [clojure-map]
    "Replace symbols and keywords by strings"
    (let [pythonise
          (fn [elem]
            (println "elem" elem)
            (cond
              (or (keyword? elem) (symbol? elem))
              (str elem)

              (map? elem)
              (map-to-python-dict elem)

              :else
              elem))]
    (zipmap (map pythonise (keys clojure-map))
            (map pythonise (vals clojure-map)))))

(defn complexity-vs-reject-ratio
  "Compare the run time versus the complexity"
  []
  (let [plot-legend-python
         "{':x-label' : 'Number of Points',
          ':y-label' : 'Reject Ratio',
          ':title' : 'Comparison of different samplers',
          ':inspect-legend' : 
          {':reject-ratio' : 'Rejection Ratio'},
           ':bucket-legend' :
          {':sample-type' :
           {':name' : 'Sampler Type',
            ':options' : {0 : 'Construct Sampler', 1 : 'Rejection Sampler'}}},
           ':remove-inconsistent?' :
           {':name' : 'Remove Inconsistent?',
            ':options' : {0 : 'Remove', 1 : 'Do not remove'}}}"
        complexity-test
        (fn [n-points]
          (let [n-samples 50
                constraint
                (avoid-orthotope-obs n-points
                                     [1 1] [9 9] 
                                     [[[3 6][0 3.5]]
                                      [[0 2][5 7]]
                                      [[4 7][5 7]]]
                                     10)
                sampler (bucket :sample-type plan-by-construct
                                             plan-by-rejection)]
            (sampler constraint n-samples)))]
    (coll-to-file
      (bucket-scaling-plot
          (bucket-test
          [:sample-type :remove-inconsistent?]
          (scaling complexity-test
                   identity
                   (map vector [3 4])
                   2))
        plot-legend-python
         [:taoensso.timbre.profiling/whole :max]
         [:taoensso.timbre.profiling/sampling-time :max])
      "zennabadman")))

(defn n-samples-vs-runtime
  ""
  []
  (let [plot-legend 
        {:x-label "Number of samples"
         :y-label "Run-time (ns)"
         :title "Comparison of different samplers"
         :inspect-legend 
         {":taoensso.timbre.profiling/whole :max" "Whole run time"
          ":taoensso.timbre.profiling/sampling-time :max" "Sampling time only"}
         :bucket-legend
         {":sample-type"
          {:name "Sampler Type"
           :options {0 "Construct Sampler" 1 "Rejection Sampler"}}}}
         plot-legend-python
         "{':x-label' : 'Number of samples',
          ':y-label' : 'Run-time (ns)',
          ':title' : 'Comparison of different samplers',
          ':inspect-legend' : 
          {':taoensso.timbre.profiling/whole :max' : 'Whole run time',
           ':taoensso.timbre.profiling/sampling-time :max' : 'Sampling time only'},
          ':bucket-legend' :
          {':sample-type' :
           {':name' : 'Sampler Type',
            ':options' : {0 : 'Construct Sampler', 1 : 'Rejection Sampler'}}}}"

          constraint
          (avoid-orthotope-obs 4
                               [1 1] [9 9] 
                               [[[3 6][0 3.5]]
                                [[0 2][5 7]]
                                [[4 7][5 7]]]
                               10)
          ]
    (coll-to-file
      (bucket-scaling-plot
        (bucket-test
          [:sample-type :remove-inconsistent?]
          (scaling (bucket :sample-type (partial plan-by-construct constraint)
                                        (partial plan-by-rejection constraint))
                   identity
                   (map vector (range 1 501 50)) 3))
        plot-legend-python
         [:taoensso.timbre.profiling/whole :max]
         [:taoensso.timbre.profiling/sampling-time :max])
      "zennabadman3")))

(defn run-all-experiments
  []
  (do
    (n-samples-vs-runtime)))