(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.constrain
  (:use relax.common)
  (:use relax.env)
  (:use relax.symbolic)
  (:use relax.conditionalvalue)
  (:use relax.multivalue)
  (:use relax.examples)
  (:use relax.linprog)
  (:use relax.abstraction)
  (:use relax.box)
  (:use clozen.helpers)
  (:use relax.evalcs)
  (:require [taoensso.timbre.profiling :as profiling :refer (p o profile)])
  (:require [clojure.math.combinatorics :as combo]))

(defn satisfiable?
  "Does a solution satisfy the constraint or subc constraint"
  [sample formula vars]
  ; (println "sample" sample "formula" formula "vars" vars)
  (let [extended-env (extend-environment vars sample the-pure-environment)]
    (every? true? (map #(evalcs % extended-env) formula))))

(defn to-dnf-new
  "Takes a program and converts it to disjunctive normal form"
  [vars model-constraints pred]

  ; Add variables to environment
  (doall
    (for [variable vars]
      (define-symbolic! variable the-global-environment)))

  (println "the the-global-environment is" the-global-environment "\n")
  (println "Original Predicate Is" pred  "\n")
  (println "vars is" vars "\n")

  (let [x (evalcs pred the-global-environment)]
    (mapv (comp vec #(if (conjun? %) (conjun-operands %) [%])) (disjun-operands x))))

(defn to-dnf
  "Takes a program and converts it to disjunctive normal form"
  [vars model-constraints pred]

  ; Add variables to environment
  (doall
    (for [variable vars]
      (define-symbolic! variable the-global-environment)))

  (println "the the-global-environment is" the-global-environment "\n")
  (println "Original Predicate Is" pred  "\n")  
  (println "the expanded predicate is"(andor-to-if pred)  "\n")

  (let [ineqs (multivalues
                (all-possible-values 
                (evalcs (andor-to-if pred)
                                            ; (conj model-constraints pred)))
                        the-global-environment)))]
    (map value-conditions
         (filter #(true? (conditional-value %)) ineqs))))

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
              (unsymbolise clause))]
    (if (some nil? (flatten (:internals box)))
        'empty-abstraction
        box)))

(defn cover
  "Cover each polytope individually"
  [clauses vars]
  ; (println  "CLAUSES" clauses)
  (let [budget 2500
        ; pvar (println "ORIGINAL BOX UNFILT" (map #(bound-clause % vars) clauses))
        large-abstrs (filterv has-volume? 
                             (map #(bound-clause % vars) clauses))
        pvar (println "After removing empty" (count large-abstrs))
        ; large-abstrs (cover-abstr large-abstrs)
        ]
    (println "After dissection" (count large-abstrs))
    (loop [abstrs large-abstrs n-iters 10]
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

(defn constrain-uniform-divisive
  "Make a sampler"
  [vars model-constraints pred]
  (let [pred-fn (make-lambda-args pred vars)
        dnf (to-dnf-new vars model-constraints pred)
        pvar (println "DNF" (count dnf))
        covers (cover dnf vars)
        volumes (map volume covers)]
    #(loop [n-sampled 0 n-rejected 0]
        (let [abstr (categorical covers volumes)
              sample (abstraction-sample abstr)]
          (if (apply pred-fn sample)
              {:sample sample :n-sampled (inc n-sampled) :n-rejected n-rejected}
              (recur (inc n-sampled) (inc n-rejected)))))))

;; Other
(defn naive-rejection
  "Just sample and accept or reject"
  [variable-intervals pred]
  (let [pred-fn (make-lambda-args pred (vec (keys variable-intervals)))]
  #(loop [n-sampled 0 n-rejected 0]
    (let [sample (interval-sample (vals variable-intervals))]
     (if (apply pred-fn sample)
         {:sample sample :n-sampled (inc n-sampled) :n-rejected n-rejected}
         (recur (inc n-sampled) (inc n-rejected)))))))

(defn take-samples
  [pred vars n-samples]
  (let [intervals (mapv #(vector `(~'> ~% 0) `(~'< ~% 10)) vars)
        new-model (constrain-uniform-divisive
                    vars
                    (reduce concat intervals)
                    pred)
        data (repeatedly n-samples new-model)
        samples (extract data :sample)
        n-sampled (sum (extract data :n-sampled))
        n-rejected (sum (extract data :n-rejected))

        ; srs-model (naive-rejection
        ;   (zipmap vars (repeat (count vars) (vector 0 10))) pred)
        ; srs-data (repeatedly n-samples srs-model)
        ; srs-samples (extract srs-data :sample)
        ; srs-n-sampled (sum (extract srs-data :n-sampled))
        ; srs-n-rejected (sum (extract srs-data :n-rejected))
        ]
        (samples-to-file "op" samples)
        ; (samples-to-file "srsop" srs-samples)
        (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
        ; (println "N-SAMPLES-SRS:" srs-n-sampled " n-rejected: " srs-n-rejected " ratio:" (double (/ srs-n-rejected srs-n-sampled)))
        samples))

(defn -main[]
  (let [{vars :vars pred :pred}
        (avoid-orthotope-obs 4 [1 1] [9 9] [[[2 5][5 7]] [[5 8][0 3]]])
        vars (vec vars)]
  (profile :info :whatevs (p :FYLL (take-samples pred vars 100)))))

(def pred-x
  '(and
     a
     b
     

     (or c d e f)
     (or g h i j)))

(def exp1
  '(or (and
        (or a b)
        c
        (and e f))
        g))


; cases, there are no disjunctions as argumenets
;; Then itll just be a case of a cojunction of terms

;; There ARE only disjunctions


;; there's a mix

; (defn -main[]
;   (let [dnf (to-dnf-new '[a b c d e f g h i j x] nil exp1)]
;     (println "count" (count dnf) "\n" dnf)))

; (defn -main[]
;   (take-samples exp-linear '[x1 x2] 100))