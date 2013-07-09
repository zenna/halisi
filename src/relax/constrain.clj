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
            (error "inequality unsatisfiable")

            (> upperb ineq-val)
            [lowerb ineq-val]

            :else
            interval)
      '<= (cond
            (< ineq-val lowerb)
            (error "inequality unsatisfiable")

            (> upperb ineq-val)
            [lowerb ineq-val]

            :else
            interval)
      '>  (cond
            (>= ineq-val upperb)
            (error "inequality unsatisfiable")

            (> ineq-val lowerb)
            [ineq-val upperb]

            :else
            interval)
      '>= (cond
            (> ineq-val upperb)
            (error "inequality unsatisfiable")

            (> ineq-val lower-bound)
            [ineq-val upperb]

            :else
            interval)
      (error "this predicate not supported"))))

(defn update-intervals
  "Compute a et of intervals with a set of inequalities"
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

  ; idiom is that given a coll, you want to use the first element to modify some object
  ; then use the output of this as the element to be modified using the second object
  ; in this case the objects are our constraints

;TODO Does this support negative intervals?
(defn sample-within-intervals
  [intervals]
  (for [interval (vals intervals)]
    (+ (lower-bound interval)
       (rand (- (upper-bound interval) (lower-bound interval))))))

; ((conditioned-value true [(symbolic (< (symbolic x2) 1)) (symbolic (<= (symbolic x2) 10)) (symbolic (> (symbolic x1) 9))])
;  (conditioned-value true [(symbolic (> (symbolic x2) 10)) (symbolic (> (symbolic x1) 9))])
;  (conditioned-value true [(symbolic (> (symbolic x2) 10)) (symbolic (<= (symbolic x1) 9))]))

(defn constrain-uniform
  "Takes a model represented as map of variables to intervals on uniform distribution"
  [variable-intervals pred]
  (doall
    (for [variable (keys variable-intervals)]
      (define-symbolic! variable the-global-environment)))
    (println "the the-global-environment is" the-global-environment "\n")
    (println "the expanded predicate is" (andor-to-if pred)  "\n")
    (let [ineqs (filter #(true? (condition-value %))
                              (multivalues
                                (evalcs (andor-to-if pred)
                                        the-global-environment)))
          intervals-disjuction (map #(update-intervals (conditions %) variable-intervals)
                                     ineqs)
          volumes (map compute-volume intervals-disjuction)
          pvar (println "Intervals distjucntion" intervals-disjuction "INEQUALITIES"  ineqs "volumes" volumes)]
      #(sample-within-intervals (categorical intervals-disjuction volumes))))

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
  '(or (and (> x1 2) (> x2 2))
       (> x2 10)))

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
       (if (< y 4) 'a 'b)
       true))


; (if (> x1 9)
;     (if (> x2 10)
;         true
;         (if (< x2 1)
;             true
;             false))
;     (if (> x2 10)))

; m [true  | (< x2 2)
;    false | (>= x2 1)]

; ->

; m [true  | (< x2 2) (<= x2 10)
;   false  ? (>= x2 1) (<= x2 10)]

(defn -main[]
  (constrain-uniform (zipmap '[x1 x2] [[0 10] [0 20]]) exp5))
  ; (let [new-model (constrain-uniform (zipmap '[x1 x2] [[0 10] [0 20]]) exp2)
  ;       samples (repeatedly 1000 new-model)]
  ;   [(vec (map first samples))
  ;    (vec (map second samples))]))