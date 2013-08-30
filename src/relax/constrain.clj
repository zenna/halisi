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
  (:require [clojure.math.combinatorics :as combo]))

(defn ineq-as-matrix
  "Takes an inequality expression, e.g. (> x 2) and converts it
   into a matrix for use with the linear programming solver"
  [exp vars]
  (let [var-id (zipmap vars (range (count vars)))
        exp (symbolic-value exp)
        second-arg (symbolic-value (second exp))
        add-sub (if (coll? second-arg)
                    (eval (operator second-arg))
                    +)
        arguments (if (coll? second-arg)
                      (rest second-arg)
                      [(make-symbolic second-arg)])]
    [(pass
        (fn [term row]
          (cond
            (tagged-list? (symbolic-value term) '*)
            (let [{num :num symb :symb} (decompose-binary-exp (symbolic-value term))]
              (assoc row (var-id (symbolic-value symb)) (* (add-sub 1) num)))

            (symbolic? term)
            (assoc row (var-id (symbolic-value term)) (add-sub 1))

            :else
            (error "UNKOWN TERM" term)))

        (vec (zeros (count vars)))
        arguments)
    
    (range 1 (inc (count vars)))
    (operator exp)
    (last exp)]))

(defn satisfiable?
  "Does a solution satisfy the constraint or subc constraint"
  [sample formula vars]
  ; (println "sample" sample "formula" formula "vars" vars)
  (let [extended-env (extend-environment vars sample the-pure-environment)]
    (every? true? (map #(evalcs % extended-env) formula))))

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
        pvar (println "BEFORE" (count large-abstrs))
        pvar (println "BEFORE" (count large-abstrs))
        large-abstrs (cover-abstr large-abstrs)]
    (println "ORIGINAL BOX" (count large-abstrs))
    (println "ORIGINAL BOX" (count large-abstrs))
    (loop [abstrs large-abstrs n-iters 10]
      (println "NUMBOXES" (count abstrs) (reduce + (map volume abstrs)))
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
        dnf (to-dnf vars model-constraints pred)
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

(defn -main[]
  (let [{vars :vars pred :pred} (gen-box-non-overlap-close 3)
        ; {vars :vars pred :pred}
        ; (avoid-orthotope-obs 3 [1 1] [9 9] [[[2 5][5 7]] [[5 8][0 3]]])
        vars (vec vars)
        ; pred exp-rand-and-3d
        n-samples 100
        ; vars '[x1 x2 x3 x4]
        intervals (mapv #(vector `(~'> ~% 0) `(~'< ~% 10)) vars)
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

; (defn -main[]
;   (let [{vars :vars pred :pred} (gen-box-non-overlap-close 3)
;         intervals (vec (reduce concat (map #(vector `(~'> ~% 0) `(~'< ~% 1)) vars)))
;         new-model (constrain-uniform-divisive
;                     (vec vars)
;                     intervals
;                     pred)
;         data (repeatedly 1 new-model)
;         samples (extract data :sample)
;         n-sampled (sum (extract data :n-sampled))
;         n-rejected (sum (extract data :n-rejected))]
;   (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
;   (samples-to-file "opx" samples)
;   samples))

(defn overlapping-sampler [n-samples]
  (let [box1 {:name 'box1 :internals [[0 20][0 20]]}
        box2 {:name 'box2 :internals [[18 22][18 22]]}
        ; box3 {:name 'box3 :internals [[0 10][0 12]]}
        vars '[x0 x1]
        boxes [box2 box1]
        formula-maker
        (fn [box]
          `(~'and
            ~@(reduce concat
                (for [[low upp i] (map #(conj %1 %2) box (range (count box)))]
              `((~'> ~(symbol (str "x" i)) ~low)
                (~'< ~(symbol (str "x" i)) ~upp))))))
        boxes (map #(assoc % :formula
                              (make-lambda-args (formula-maker (:internals %)) vars))
                    boxes)
        volumes (map volume boxes)]
    (loop [samples [] cache (zipmap boxes []) n-samples n-samples]
      (println cache "\n")
      (cond
        (zero? n-samples)
        samples

        :else
        (let [box (categorical boxes volumes)
              cache-item (peek (cache box))]
          ; Do I have a cache?
          (if (nil? cache-item)
              ; If not proceed normally
              (let [sample (interval-sample (:internals box))
                    all-boxes (map #(apply (:formula %) sample) boxes)
                    pos-dominant (max-pred-index true? all-boxes)
                    pos-box (max-pred-index #(= box %) boxes)]
                    (if (= pos-dominant pos-box)
                        (recur (conj samples sample) cache (dec n-samples))
                        (recur samples cache n-samples)))
                        ; (recur samples
                        ;        (update-in cache [(nth boxes pos-dominant)]
                        ;                   #(conj % {:from box :sample sample}))
                        ;        n-samples)))

              ; Otherwise with prob p=ratio of overlapping region/box, take sample
              (if (flip (/ (volume (overlap box (:from cache-item))) (volume box)))
                  (recur (conj samples (:sample cache-item)) ; I should take the sample
                         (update-in cache [box]
                                    #(pop %))
                         (dec n-samples))
                  ; and 1-p generate sample in non-overlapping region
                  ; Treat as normal sample
                  (let [sample (gen-until #(interval-sample (:internals box))
                                    #(and (apply (:formula box) %)
                                          (not (apply (:formula (:from cache-item)) %))))]
                        ; all-boxes (map #(apply (:formula %) sample) boxes)
                        ; pos-dominant (max-pred-index true? all-boxes)
                        ; pos-box (max-pred-index #(= box %) boxes)]
                        ; (if (= pos-dominant pos-box)
                            (recur (conj samples sample) cache (dec n-samples))))))))))
                            ; (recur samples
                            ;        (update-in cache [(nth boxes pos-dominant)]
                            ;                   #(conj % {:from box :sample sample}))
                            ;        n-samples))))))))))

; (defn -main []
;   (samples-to-file "kidding2" (overlapping-sampler 20000)))