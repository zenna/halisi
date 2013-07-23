(ns ^{:doc "Constrain a generative model"
      :author "Zenna Tavares"}
  relax.constrain
  (:use relax.evalcs)
  (:use relax.symbolic)
  (:use relax.common)
  (:use relax.examples)
  (:use relax.env)
  (:use relax.linprog)
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

;; Inequality abstractions
(defn decompose-binary-exp
  "Takes a binary exp involving a symbol and a concrete number
   and extracts them into a map.

   Useful to find the symbol and/or number when an expression could be
   (+ x 2) or (+ 2 x) for instnace"
  [ineq]
  (cond
    (symbolic? (nth ineq 1))
    {:num (nth ineq 2) :symb (nth ineq 1)}

    (symbolic? (nth ineq 2))
    {:num (nth ineq 1) :symb (nth ineq 2)}

    :else
    (error "one of the values in inequality must be symbolic")))

(defn lower-bound [interval]
  (first interval))

(defn upper-bound [interval]
  (second interval))

;TODO Does this support negative intervals?
(defn interval-sample
  "Sample within box"
  [intervals]
  (for [interval intervals]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

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

(defn unsymbolise
  [formula]
  "Remove symbols from something like this:
  (<= (symbolic (+ (symbolic (* -4 (symbolic x1))) (symbolic x2))) 10)"
  (map 
    #(let [value (if (symbolic? %)
                     (symbolic-value %)
                     %)]
      (if (coll? value)
          (unsymbolise value)
          value))
    formula))

(defn conjoin
  "Conjoin an expression"
  [& exprs]
  `(~'and ~@exprs))

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

(defn make-abstraction
  "An abstraction has a formula, which is purely conjunctive, and can be evaluated
   using satisfiable?.  It also has some internal structure which depends on its type"
  [internals formula]
  {:internals internals :formula formula})

;; Box (Orthotope) abstractions
(defn middle-split
  [box]
  (map #(double (+ (lower-bound %) (/ (- (upper-bound %) (lower-bound %)) 2))) box))

(defn split
  "Split a box into 2^D other boxes"
  [box split-points]
  (map
    #(make-abstraction % (:formula box)) ; All subboxes have same formula as parent
    (for [dim-to-change (apply combo/cartesian-product (:internals box))]
      (mapv
        (fn [dim-to-replace min-max split-point]
          (vec (sort [(first (filter #(not= dim-to-replace %) min-max)) split-point])))
        dim-to-change (:internals box) split-points))))

(defn split-uniform
  "Split the box into equally sized boxes"
  [box]
  (split box (middle-split (:internals box))))

(defn bound-clause
  [clause vars]
  ; (println "clause" clause "vars" vars)
  (let [interval-constraints (map #(evalcs % the-global-environment)
                                   (vec 
                                    (reduce concat (map #(vector `(~'> ~% 0) `(~'< ~% 1)) vars))))
        ; pvar (println "interval constraints" interval-constraints)
        box (make-abstraction
              (partition 2
                         (bounding-box-lp
                           (map #(ineq-as-matrix % vars)
                                 (concat clause interval-constraints))
                            vars))
              (unsymbolise clause))]
    (if (some nil? (flatten (:internals box)))
        'empty-abstraction
        box)))

(defn abstraction-vertices
  "Get the vertices of an abstraction"
  [box]
  (apply combo/cartesian-product (:internals box)))

(defn completely-within?
  [box vars]
  (every? #(satisfiable? % (:formula box) vars) (abstraction-vertices box)))

(defn on-boundary?
  [box vars]
  (not (completely-within? box vars)))

(defn volume
  "get the box volume"
  [box]
  (apply * (map #(- (upper-bound %) (lower-bound %)) (:internals box))))

(defn formula
  "Get formula of abstraction"
  [abstraction]
  {:post [(not (nil? %))]}
  (:formula abstraction))

(defn non-empty-abstraction?
  [abstraction vars]
  "Is the box not empty? Box can be empty because we find it infeasible
   Or due to subdivison process"
  (and
    (not= abstraction 'empty-abstraction)
    (some #(satisfiable? % (formula abstraction) vars) (abstraction-vertices abstraction))))

(defn has-volume?
  [abstraction]
  "Does the box have volume? Box may not have volume infeasible"
  (not= abstraction 'empty-abstraction))

(defn cover
  "Cover each polytope individually"
  [clauses vars]
  ; (println  "CLAUSES" clauses)
  (let [budget 100000
        large-abstrs (filter has-volume? 
                             (map #(bound-clause % vars) clauses))]
    (println "ORIGINAL BOX" large-abstrs)
    (println "ORIGINAL BOX" large-abstrs)
    (loop [abstrs large-abstrs n-iters 100]
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

(defn abstraction-sample
  [box]
  (for [interval (:internals box)]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

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
        vars (vec vars)
        ; pred exp-rand-and-3d
        n-samples 20
        ; vars '[x1 x2 x3 x4]
        intervals (mapv #(vector `(~'> ~% 0) `(~'< ~% 1)) vars)
        new-model (constrain-uniform-divisive
                    vars
                    (reduce concat intervals)
                    pred)
        data (repeatedly n-samples new-model)
        samples (extract data :sample)
        n-sampled (sum (extract data :n-sampled))
        n-rejected (sum (extract data :n-rejected))

        srs-model (naive-rejection
          (zipmap vars (repeat (count vars) (vector 0 1))) pred)
        srs-data (repeatedly n-samples srs-model)
        srs-samples (extract srs-data :sample)
        srs-n-sampled (sum (extract srs-data :n-sampled))
        srs-n-rejected (sum (extract srs-data :n-rejected))]
        (samples-to-file "op" samples)
        (samples-to-file "srsop" srs-samples)
        (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
        (println "N-SAMPLES-SRS:" srs-n-sampled " n-rejected: " srs-n-rejected " ratio:" (double (/ srs-n-rejected srs-n-sampled)))
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