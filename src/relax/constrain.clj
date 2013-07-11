(ns ^{:doc "A constrain a generative model"
      :author "Zenna Tavares"}
  relax.constrain
  (:use relax.evalcs)
  (:use relax.symbolic)
  (:use relax.common)
  (:use clozen.helpers))

;; Inequality abstractions
(defn decompose-inequality
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

(defn revise-interval
  "Constrain an interval with an inequality.
   e.g. (interval [0 10] '> 5) should eval to [0 5]"
  [interval ineq-pred ineq-val]
  {:pre [(>= (upper-bound interval) (lower-bound interval))]
   :post [(>= (upper-bound %) (lower-bound %))]}
  ; (println "INTERVAL" interval "ineq-pred" ineq-pred "ineq-val" ineq-val "\n")
  (let [lowerb (lower-bound interval)
        upperb (upper-bound interval)]
    (condp = ineq-pred
      '<  (cond
            (<= ineq-val lowerb)
            [0.0 0.0] ;(error "inequality unsatisfiable")

            (> upperb ineq-val)
            [lowerb ineq-val]

            :else
            interval)
      '<= (cond
            (< ineq-val lowerb)
            [0.0 0.0] ;(error "inequality unsatisfiable")

            (> upperb ineq-val)
            [lowerb ineq-val]

            :else
            interval)
      '>  (cond
            (>= ineq-val upperb)
            [0.0 0.0] ; (error "inequality unsatisfiable")

            (> ineq-val lowerb)
            [ineq-val upperb]

            :else
            interval)
      '>= (cond
            (> ineq-val upperb)
            [0.0 0.0] ;(error "inequality unsatisfiable")

            (> ineq-val lowerb)
            [ineq-val upperb]

            :else
            interval)
      (error "this predicate not supported"))))

(defn update-intervals
  "Compute a set of intervals with a set of inequalities"
  [ineqs variable-intervals]  
  (pass (fn [ineq var-intervals]
          ; (println "DEBUG ineq" ineq "var-intervals" var-intervals "\n") 
          (let [ineq (symbolic-value ineq)
                {num :num symb :symb} (decompose-inequality ineq)]
            ; (println "DEBUG num" num "symb" symb "\n")
            (update-in var-intervals [(symbolic-value symb)]
                       #(revise-interval % (first ineq) num))))
        variable-intervals
        ineqs))

;TODO
(defn compute-volume
  "Compute the volumes of the orthotope (hyperrectangle)"
  [intervals]
  (reduce * (map #(- (upper-bound %) (lower-bound %))
                  (vals intervals))))

;TODO Does this support negative intervals?
(defn sample-within-intervals
  "Sample within box"
  [intervals]
  (for [interval (vals intervals)]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

(defn bound-polytope
  "Compute a suitable bounding volume of the polytope"
  [polytope]

(defn cover
  ""
  [ineqs]
  (map bound-polytope ineqs))

(defn constrain-uniform
  "Takes a model represented as map of variables to intervals on uniform distribution"
  [variable-intervals pred]
  
  ; Add variables to environment
  ; TODOconvert given intervals to constraints
  (doall
    (for [variable (keys variable-intervals)]
      (define-symbolic! variable the-global-environment)))

  (println "the the-global-environment is" the-global-environment "\n")
  (println "the expanded predicate is" (andor-to-if pred)  "\n")

  (let [ineqs   (multivalues (all-possible-values 
                  (evalcs (andor-to-if pred) the-global-environment)))
        ineqs (filter #(true? (conditional-value %)) ineqs)
        pvar (println "DISJUNCTIVE NORMAL FORM:" ineqs "\n")

        ; Then for each disjuctive clause we need to compute an abstraction
        containers (cover ineqs)
        volumes (map container-volume)
        pvar (println "Intervals distjucntion" intervals-disjuction "INEQUALITIES"  ineqs "volumes" volumes)]
    #(sample-within-intervals (categorical intervals-disjuction volumes))))

; (defn constrain-uniform
;   "Takes a model represented as map of variables to intervals on uniform distribution"
;   [variable-intervals pred]
  
;   ; Add variables to environment
;   ; TODOconvert given intervals to constraints
;   (doall
;     (for [variable (keys variable-intervals)]
;       (define-symbolic! variable the-global-environment)))

;   (println "the the-global-environment is" the-global-environment "\n")
;   (println "the expanded predicate is" (andor-to-if pred)  "\n")

;   (let [ineqs   (multivalues (all-possible-values 
;                   (evalcs (andor-to-if pred) the-global-environment)))
;         ineqs (filter #(true? (conditional-value %)) ineqs)
;         pvar (println "DISJUNCTIVE NORMAL FORM:" ineqs "\n")

;         ; Then for each disjuctive clause we need to compute an abstraction
;         containers (cover ineqs)
;         intervals-disjuction (map #(update-intervals (value-conditions %)
;                                                      variable-intervals)
;                                    ineqs)
;         volumes (map compute-volume intervals-disjuction)
;         pvar (println "Intervals distjucntion" intervals-disjuction "INEQUALITIES"  ineqs "volumes" volumes)]
;     #(sample-within-intervals (categorical intervals-disjuction volumes))))

(def exp 
  '(if (> x1 9)
      (or (> x2 10)
          (< x2 1))
      (if (> x2 8)
          true
          false)))

(def exp2
  '(if (> x1 8)
      (or (> x2 10)
          (< x2 1))
      (if (> x2 5)
          (or (> x2 7)
              (< x1 9))
          false)))

(def exp3
  '(or (and (> x1 7) (> x2 7) (< x1 9) (< x2 10))
       (and (> x1 3) (> x2 3) (< x1 5) (< x2 5))
       (< x1 1)))

(def exp7
  '(if (> x1 2)
        (if (> x2 2)
            true
            false)
        false))

(def exp10
  '(if (> x1 2)
        true
        false))

(def exp4
  '(if (if (> x1 3)
            true
            false)
      false
      true))

(def exp5
  '(if (if (> x1 3)
           true
           false) 
       (if (< x2 4) true false  )
       true))

(defn -main[]
  (let [new-model (constrain-uniform (zipmap '[x1 x2] [[0 10] [0 20]]) exp3)
        samples (repeatedly 1000 new-model)]
    [(vec (map first samples))
     (vec (map second samples))]))