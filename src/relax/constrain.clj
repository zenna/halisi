(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.constrain
  (:require [relax.common :refer :all]
            [relax.env :refer :all]
            [relax.symbolic :refer :all]
            [relax.conditionalvalue :refer :all]
            [relax.multivalue :refer :all]
            [relax.linprog :refer :all]
            [relax.abstraction :refer :all]
            [relax.domains.box :refer :all]
            [relax.evalcs :refer :all])
  (:require [clojure.math.combinatorics :as combo])
  (:require [clozen.helpers :as clzn :refer :all]
            [clozen.debug :refer [dbg]]
            [fipp.edn :refer (pprint) :rename {pprint fipp}]
            [taoensso.timbre.profiling :as profiling :refer (p o profile)]))

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
  (println "Original Predicate Is" (fipp pred)  "\n")
  (println "vars is" vars "\n")

  (let [x (evalcs pred the-global-environment)]
    (mapv (comp vec #(if (conjun? %) (conjun-operands %) [%])) (disjun-operands x))))

(defn bound-clause
  "Take a clause (from dnf) and find bounding box"
  [clause vars prior]
  ; (println "clause" clause "vars" vars)
  (let [interval-constraints (map #(evalcs % the-global-environment)
                                   (vec 
                                    (reduce concat
                                      (map 
                                      (fn [a-var [l u]]
                                        (vector `(~'> ~a-var ~l) `(~'< ~a-var ~u))) vars prior)))) ;HACK
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
  [clauses vars prior]
  (println "Covering Polytopes individually")
  (let [budget 2500
        large-abstrs (filterv #(and (has-volume? %) (not (zero? (volume %))))
                             (map #(bound-clause % vars prior) clauses))
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
  [vars prior pred]
  (let [pred-fn (make-lambda-args (long-args-to-let pred vars) '[sample])
        pvar (println "PRED" (long-args-to-let pred vars))
        dnf (to-dnf-new vars pred)
        pvar (println "DNF" (count dnf))]
    (cover dnf vars prior)))

(defn abstract-to-sampler
  "Take an abstract object and make a sampler out of it"
  [abstracts vars pred]
  (let [pred-fn (make-lambda-args (long-args-to-let pred vars) '[sample])
        volumes (map volume abstracts)]
    #(loop [n-sampled 0 n-rejected 0]
            (let [abstr (categorical abstracts volumes)
                  sample (abstraction-sample abstr)]
              (if (pred-fn sample)
                  (o :reject-ratio ; Profile the rejection count
                      (fn [{n-rejected :n-rejected  n-sampled :n-sampled}]
                        (/ n-rejected (+ n-rejected n-sampled)))
                    {:sample sample
                     :n-sampled (inc n-sampled)
                     :n-rejected n-rejected})
                  (recur (inc n-sampled) (inc n-rejected)))))))

(defn construct
  "Do Abstract Interpretation and then convert abstract object into a sampler
   vars is a set of symbols with variable names
   prior is a set of intervals"
  [vars prior pred]
  {:pre [(clzn/count= prior vars)]}
  (abstract-to-sampler
    (construct-box-domain vars prior pred)
    vars pred))

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
  "Constructs a sampler using construct and writes results
   to file"
  [vars prior pred n-samples]
  (let [new-model (construct
                    vars
                    prior
                    pred)
        pvar (println "Taking Samples...")
        data (repeatedly n-samples new-model)
        samples (extract data :sample)
        n-sampled (sum (extract data :n-sampled))
        n-rejected (sum (extract data :n-rejected))]
        (samples-to-file "op" samples)
        (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
        samples))