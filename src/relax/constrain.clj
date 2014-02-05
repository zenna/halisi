(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.constrain
  (:require [relax.common :refer :all]
            [relax.env :refer :all]
            [relax.symbolic :refer :all]
            [relax.conditionalvalue :refer :all]
            [relax.multivalue :refer :all]
            [relax.examples :refer :all]
            [relax.linprog :refer :all]
            [relax.abstraction :refer :all]
            [relax.domains.box :refer :all]
            [relax.evalcs :refer :all]
            [clozen.helpers :refer :all]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]
            [clojure.math.combinatorics :as combo]))

; The variables have some history
; Originally I would add on at the end when I found the bounding box using lp
; Then I changed it so that I included them in the actual code
; This was problemantic as I got some kind of stack overflow
; But then I changed to and or model. and now I am not sure what the current status is.

(defn satisfiable?
  "Does a solution satisfy the constraint or subc constraint"
  [sample formula vars]
  ; (println "sample" sample "formula" formula "vars" vars)
  (let [extended-env (extend-environment vars sample the-pure-environment)]
    (every? true? (map #(evalcs % extended-env) formula))))

(defn to-dnf-new
  "Evaluates pred using abstract interpretation.
   vars - list of symbols which the interpreter should treat as variables"
  [vars pred]

  ; Add variables to environment
  (doall
    (for [variable vars]
      (define-symbolic! variable the-global-environment)))

  (println "the the-global-environment is" the-global-environment "\n")
  (println "Original Predicate Is" pred  "\n")
  (println "vars is" vars "\n")

  (let [x (evalcs pred the-global-environment)]
    (mapv (comp vec #(if (conjun? %) (conjun-operands %) [%])) (disjun-operands x))))

(defn bound-clause
  "Take a clause (from dnf) and find bounding box"
  [clause vars]
  ; (println "clause" clause "vars" vars)
  (let [interval-constraints (map #(evalcs % the-global-environment)
                                   (vec 
                                    (reduce concat (map #(vector `(~'> ~% 0) `(~'< ~% 10)) vars)))) ;HACK
        ; pvar (println "interval constraints" interval-constraints)
        box (make-abstraction
              (mapv vec (partition 2
                         (bounding-box-lp
                           (mapv #(ineq-as-matrix % vars)
                                 (concat clause interval-constraints))
                            vars)))
              (unsymbolise clause))
        box (merge box {:formula-as-matrix
                        (mapv #(ineq-as-matrix % vars)
                              (concat clause interval-constraints))
                        :vars vars})]
    (if (some nil? (flatten (:internals box)))
        'empty-abstraction
        box)))

(defn abstraction-sample-cheat
  "Sample with the abstraction"
  [box]
  (let [lp-point (arbitrary-point-lp (:formula-as-matrix box) (:vars box))]
    lp-point))

(defn cover
  "Cover each polytope individually"
  [clauses vars]
  (println "Covering Polytopes individually")
  (let [budget 2500
        large-abstrs (filterv #(and (has-volume? %) (not (zero? (volume %))))
                             (map #(bound-clause % vars) clauses))
        pvar (println "After removing empty" (count large-abstrs))
        ; large-abstrs (cover-abstr large-abstrs)
        ]
    (println "After dissection" (count large-abstrs))
    (loop [abstrs large-abstrs n-iters  0]
      ; (println "NUMBOXES" (count abstrs)
      ;   (let [sum-vol (reduce + (map volume abstrs))
      ;         union-vol (apply union-volume abstrs)]
      ;     [sum-vol union-vol (/ union-vol sum-vol)]))
      ; (println "NEWBOX" abstrs)

      (cond
        (zero? n-iters)
        abstrs

        (> (count abstrs) budget) ; Overbudget => Stop
        abstrs

        (empty? (filter #(on-boundary? % vars) abstrs)) ; Perfect covering => Stop
        abstrs

        :else
        (let [f-intersects-b (filter #(on-boundary? % vars) abstrs)
              to-split (categorical f-intersects-b (map volume f-intersects-b))
              splitted (split-uniform to-split)
              new-abstrs (vec (concat splitted (remove #(= to-split %)
                                                        abstrs)))]
          (recur (filter #(non-empty-abstraction? % vars) new-abstrs) (dec n-iters)))))))

(defn construct-box-domain
  "Interpret pred abstractly with box domain"
  [vars pred]
  (let [pred-fn (make-lambda-args (long-args-to-let pred vars) '[sample])
        pvar (println "PRED" (long-args-to-let pred vars))
        dnf (to-dnf-new vars pred)
        pvar (println "DNF" (count dnf))]
    (cover dnf vars)))

(defn constrain-uniform-divisive
  "Make a sampler"
  [vars pred]
  (let [;pred-fn (make-lambda-args pred vars)
        pred-fn (make-lambda-args (long-args-to-let pred vars) '[sample])
        pvar (println "PRED" (long-args-to-let pred vars))
        dnf (to-dnf-new vars pred)
        pvar (println "DNF" (count dnf))
        covers (cover dnf vars)
        volumes (map volume covers)]
    #(loop [n-sampled 0 n-rejected 0]
        (let [abstr (categorical covers volumes)
              sample (abstraction-sample-cheat abstr)]
          (if (pred-fn sample)
              (o :reject-ratio ; Profile the rejection count
                  (fn [{n-rejected :n-rejected  n-sampled :n-sampled}]
                    (/ n-rejected (+ n-rejected n-sampled)))
                {:sample sample
                 :n-sampled (inc n-sampled)
                 :n-rejected n-rejected})
              (recur (inc n-sampled) (inc n-rejected)))))))

  ;; Other
(defn make-uniform-prior
  "Construct a uniform prior from some symbol names and intervals.
   This is a hack which should, and will eventually be done through normal
   abstract interpretation
   vars - list of variables, e.g. [x1 x2 x3]. Order matters
   var - map from variable to [lower-bound upper-bound]"
   [vars var-intervals]
   #(interval-sample (map var-intervals vars)))

;; Other
(defn naive-rejection
  "Return a sampler that will keep generating sample from prior until it
   satisfies pred"
  [vars pred prior]
  (let [pred-fn (make-lambda-args pred vars)]
    #(loop [n-sampled 0 n-rejected 0]
      (let [sample (prior)]
       (if (apply pred-fn sample)
           (o :reject-ratio ; Profile the rejection count
                  (fn [{n-rejected :n-rejected  n-sampled :n-sampled}]
                    (/ n-rejected (+ n-rejected n-sampled)))
                {:sample sample
                 :n-sampled (inc n-sampled)
                 :n-rejected n-rejected})
           (recur (inc n-sampled) (inc n-rejected)))))))

(defn take-samples
  "Constructs a sampler using constrain-uniform-divisive and writes results
   to file"
  [pred vars n-samples]
  (let [;intervals (mapv #(vector `(~'> ~% 0) `(~'< ~% 10)) vars)
        new-model (constrain-uniform-divisive
                    vars
                    pred)
        pvar (println "Taking Samples...")
        data (repeatedly n-samples new-model)
        samples (extract data :sample)
        n-sampled (sum (extract data :n-sampled))
        n-rejected (sum (extract data :n-rejected))]
        (samples-to-file "op" samples)
        ; (samples-to-file "srsop" srs-samples)
        (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
        ; (println "N-SAMPLES-SRS:" srs-n-sampled " n-rejected: " srs-n-rejected " ratio:" (double (/ srs-n-rejected srs-n-sampled)))
        samples))