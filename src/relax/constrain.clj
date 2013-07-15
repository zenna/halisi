(ns ^{:doc "A constrain a generative model"
      :author "Zenna Tavares"}
  relax.constrain
  (:use relax.evalcs)
  (:use relax.symbolic)
  (:use relax.common)
  (:use relax.linprog)
  (:use clozen.helpers))

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
                {num :num symb :symb} (decompose-binary-exp ineq)]
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
(defn interval-sample
  "Sample within box"
  [intervals]
  (for [interval intervals]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

(defn bound-polytope
  "Compute a suitable bounding volume of the polytope"
  [polytope])

(defn cover
  ""
  [ineqs]
  (map bound-polytope ineqs))

(defn enumerate-vertices
  [constraints])

(defn bounding-box
  "Expects data in poly-form form 
   [[x1 y1 z1][x2 y2 z2]]"
  [points]
  (pass
    (fn [point box]
      ; {:pre [(count= point box])}
      (println point "point-" "box" box)
      (for [[[lowerb upperb]  x] (map vector box point)]
        (cond
          (> x upperb)
          [lowerb x]

          (< x lowerb)
          [x upperb]

          :else
          [lowerb upperb])))
    (mapv #(vector % %) (first points)) ;bounding box
    points))

(defn box-volume
  "get the box volume"
  [box]
  (apply * (map #(- (second %) (first %)) box)))

(defn satisfiable?
  [sample ineqs])

; (>= (symbolic (+ (symbolic x1) (symbolic (* 6 (symbolic x2))))) 2)

(defn ineq-to-matrix-form
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
    ; (println "second arg" second-arg "exp" exp "args" arguments "\n")
    [(pass
        (fn [term row]
          ; (println "TERM" term "rpw " row)
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

([(10.0 7.0 9.0 7.0) (possible-value true [(symbolic (< (symbolic x2) 10)) (symbolic (< (symbolic x1) 9)) (symbolic (> (symbolic x2) 7)) (symbolic (> (symbolic x1) 7))])] 


  [(5.0 3.0 5.0 3.0) (possible-value true [(symbolic (< (symbolic x2) 5)) (symbolic (< (symbolic x1) 5)) (symbolic (> (symbolic x2) 3)) (symbolic (> (symbolic x1) 3)) (symbolic (<= (symbolic x1) 7))])] 


  [(20.0 0.0 1.0 0.0) (possible-value true [(symbolic (< (symbolic x1) 1)) (symbolic (<= (symbolic x1) 3)) (symbolic (<= (symbolic x1) 7))])])


(defn disjoint-poly-box-sampling
  [volumes formulae]
  {:pre [(count= volumes formulae)]}
  #(let [[polytope box] (categorical formulae volumes)
         sample (interval-sample box)]
     (if (satisfiable? sample polytope)
         sample
         (recur))))

(defn constrain-uniform
  "Takes a model represented as map of variables to intervals on uniform distribution"
  [variable-intervals pred]
  
  ; Add variables to environment
  (doall
    (for [variable (keys variable-intervals)]
      (define-symbolic! variable the-global-environment)))

  (println "the the-global-environment is" the-global-environment "\n")
  (println "the expanded predicate is" (andor-to-if pred)  "\n")
  (let [ineqs   (multivalues (all-possible-values 
                  (evalcs (andor-to-if pred) the-global-environment)))
        ineqs (filter #(true? (conditional-value %)) ineqs)
        interval-constraints (map #(evalcs % the-global-environment)
                                   (vec 
                                    (reduce concat (vals variable-intervals))))
        pvar (println "INEQS" (map value-conditions ineqs) "\n" "constrs" interval-constraints)
        vars (keys variable-intervals)
        matrix-form (map #(map (fn [x] (ineq-to-matrix-form x vars))
                                 (concat
                                  (value-conditions %)
                                  interval-constraints))
                          ineqs)
        pvar (println "matrix form is" (nth matrix-form 5))
        ; Then for each disjuctive clause we need to compute an abstraction
        boxes (map bounding-box-lp matrix-form)
        feasible-boxes (filter #(not-any? nil? (first %))
                                (map vector boxes ineqs))
        volumes (map #(vector (box-volume (partition 2 (first %)))
                              (value-conditions (second %)))
                      feasible-boxes)]
    (disjoint-poly-box-sampling volumes formu)
    feasible-boxes)))

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

  ; (define-symbolic! 'x1 the-global-environment)
  ; (define-symbolic! 'x2 the-global-environment)
  ; (ineq-to-matrix-form (evalcs '(>= (+ (* 6 x1) x2) 5) the-global-environment) '[x1 x2])
  ; (ineq-to-matrix-form (evalcs '(>= x2 10) the-global-environment) '[x1 x2])


(defn -main[]
  (println "op is "(constrain-uniform {'x1 ['(> x1 0) '(< x1 10)]
                                        'x2 ['(> x2 0) '(< x2 20)]}
                                        exp3)))
  ; (let [new-model (constrain-uniform {'x1 ['(> x1 0) '(< x1 10)]
  ;                                     'x2 ['(> x2 0) '(< x2 20)]}
  ;                                     exp3)
  ;       samples (repeatedly 1000 new-model)]
  ;   [(vec (map first samples))
  ;    (vec (map second samples))]))