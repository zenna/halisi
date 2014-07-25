(ns ^{:doc "Piecewise linear domain using non-linear constraint solving"
      :author "Zenna Tavares"}
  sigma.domains.line
  (:require [clozen.helpers :as clzn]
            [clozen.geometry.line :refer [->Line]]
            [clozen.vector :refer [->VectorN s* add-scaled-v v- dist]]
            [veneer.pattern.dsl-macros :refer [defrule]]
            [backtick :refer [template]]
            [clojure.java.shell :refer [sh]]))

;; TODO, make constraint on measure of image
;; Combine all constraints together
;; Add setup information
;; Send to solver
;; FIXME: Concrete overlap computation misses case where interval subsumed by other, i think

(defrecord PiecewiseLinear
  [points])

(defn linear-interpolate
  "Linearly interpolate between two points to produce a path of"
  [p0 pn n-points]
  (let [delta-v (s* (double (/ 1 (inc n-points))) (v- pn p0))]
    (mapv #(add-scaled-v p0 delta-v %) (range 0 (+ n-points 2)))))

(defn ->PiecewiseLinear
  "We will represent a distribution as a set of piecewise linear path"
  [points]
  (PiecewiseLinear. points))

(defn ->uniform-real
  "Create a uniform distribution between a and b with p points"
  [a b n]
  {:pre [(> b a)]}
  (let [height (/ 1 (- b a))]
    (->PiecewiseLinear (linear-interpolate (->VectorN 2 [height a])
                                           (->VectorN 2 [height b]) n))))

(defn interpolate
  "Inclusive interpolation of n-points between low and up"
  [low up n]
  (mapv #(+ low (* % (double (/ (- up low) n)))) (range (inc n))))

(defn p-axis
  "indepdent axis - for lack of a better term"
  [point]
  (first (:coords point)))

(defn i-axis
  "Probability axis - for lack of a better term"
  [point]
  (last (:coords point)))

(defn lower-bound
  "Get the lower bound of the independent axis"
  [points]
  (apply min (mapv i-axis points)))

(defn upper-bound
  "Get the upper bound of the independent axis"
  [points]
  (apply max (mapv i-axis points)))

(defn rand-interval
  "Uniformly sample floating between low and up"
  [low up]
  (+ low (rand (- up low))))

(defn volume
  "Hausdorf measure of trapezoids between by lines and p-axis"
  [points]
  (clzn/sum
   (map (fn [[a b]] (* 0.5 (dist a b)
                           (+ (p-axis a)
                              (p-axis b))))
        (partition 2 1 points))))

(defn between?
  "is the point within the interval?
   TODO: What about boundaries?"
  [x [low up]]
  (and (>= x low) (<= x up)))

(defn subsumes?
  "Does p subsume q?
   Assumes intervals are well-ordere"
    [[p1 p2] [q1 q2 ]]
  (and (>= q1 p1) (<= q2 p2)))

(defn overlap?
  "Do intervals p and q overlap?"
  [p [q1 q2 ]]
  (or (between? q1 p) (between? q2 p)))

(defn order-interval
  "Make lower bound of interval first in list"
  [[x y :as interval]]
  (if (>= y x) interval [y x]))

(defn project
  "Project for element of cartesian project to dim dimension"
  [v dim]
  (nth (:coords v) dim))

(defn fraction-overlap
  "What fraction of trapezoid q is overlapping with p"
  [[p1 p2 :as p] [q1 q2 ]]
  (let [[in-q out-q] (if (between? q1 p) [q1 q2] [q2 q1])]
    (if (> in-q out-q)
        (/ (- in-q p1) (- in-q out-q))
        (/ (- p2 in-q) (- out-q in-q)))))

(defn cond-expectation
  "What is the volume of the points within an interval"
  [interval points dim]
  (loop [h 0.0 pairs (partition 2 1 points)]
    (if (seq pairs)
        (let [dim-interval (mapv #(project % dim) (first pairs))
              pvar dim-interval]

          ; Three cases: subsumption, overlap, disjoint
          (cond
            (subsumes? interval (order-interval dim-interval))
            (recur (+ h (volume (first pairs))) (next pairs))

            (overlap? interval dim-interval)
            (recur (+ h (* (volume (first pairs))
                           (fraction-overlap interval dim-interval)))
                   (next pairs))

            :else
            (recur h (next pairs))))
      h)))



(defmulti lift-apply
  (fn [f & args] f))

(defmethod lift-apply 'sqr [f & args]
  (let [x (first args)]
    (order-interval (mapv #(* % %) x))))

(defmethod lift-apply :default [f & args]
  (throw (IllegalArgumentException. "Unknown lift")))



(defn rand-n-intervals
  "For a set of points, create n random intervals between the lower and upper bound"
  [points n]
  (repeatedly n
    (fn [] (order-interval
             (repeatedly 2 #(rand-interval
                              (lower-bound points)
                              (upper-bound points)))))))

;; ============================================================
;; Symbolic versions of above functions for use with smt solver
(defn symbolic-p-axis
  [point]
  (first point))

(defn symbolic-i-axis
  [point]
  (last point))

(defn symbolic-d-axis
  "Dependent axis in 3 dim random variable"
  [point]
  {:pre [(= (count point) 3)]}
  (second point))

(defn symb-vars-axes
  [v]
  (subvec v 1))

(defn symb-f
  "arbitrary symbolic f"
  [f & args]
  (template (~f ~@args)))

(def symb- (partial symb-f '-))
(def symb+ (partial symb-f '+))
(def symb* (partial symb-f '*))
(def symb-div (partial symb-f '/))

(defn symb-sqr [x]
  (symb* x x))
(defn symb-sqr-dist
  "Squared Euclidean distance - because dReal doesn't support sqrt"
  [v1 v2]
  (apply symb+ (mapv (comp symb-sqr symb-)  v1 v2)))

(defn symbolic-volume
  "Symbolic Hausdorf measure of trapezoids between by lines and p-axis
   Points is a vector of symbols [[p1 x y z][p2 x2 y2 yz]]
   Returns symbolic volume, new variables it creates, and assertions on those variables"
  [points]
  {:pre [(> (count points) 1)]}
  (loop [volumes [] variables [] assertions [] pairs (partition 2 1 points)]
    (if (seq pairs)
        (let [[a b :as pair] (first pairs)
              sqr-dist (symb-sqr-dist (symb-vars-axes a) (symb-vars-axes b))
              dist-sym (gensym "dist")
              dist-assert (template (= ~sqr-dist (* ~dist-sym ~dist-sym)))
              volume
             (symb* 0.5 dist-sym (symb+ (symbolic-p-axis a) (symbolic-p-axis b)))]
          (recur (conj volumes volume) (conj variables dist-sym) (conj assertions dist-assert) (next pairs)))
        {:volume
         (if (= (count volumes) 1)
            (first volumes)
            (apply symb+ volumes))
         :variables variables
         :assertions assertions})))

(defn subsumes?
  "Does p subsume q?
   Assumes intervals are well-ordere"
    [[p1 p2] [q1 q2 ]]
  (and (>= q1 p1) (<= q2 p2)))

(defn symbolic-subsumes?
  [[p1 p2] [q1 q2 ]]
  (template (and (>= ~q1 ~p1) (<= ~q2 ~p2))))

(defn symbolic-between?
  "is the point within the interval?
   TODO: What about boundaries?"
  [x [low up]]
  (template (and (>= ~x ~low) (<= ~x ~up))))

(defn symbolic-disjoint?
  [[p1 p2] [q1 q2]]
  (template (or (and (> ~q1 ~p2) (> ~q2 ~p2))
                (and (< ~q1 ~p1) (< ~q1 p1)))))

(defn symbolic-project
  "Cartesian projection of symbolic vector v"
  [v dim]
  (nth v dim))

(defn handle-oo
  [last-sum concrete-measure sum-symbs error-bound constraints]
;;   (apply
;;    (partial merge-with concat)
    (merge-with concat
      (update-in
       (apply (partial merge-with concat) constraints)
       [:assertions]
       #(conj % (template (< (* (- ~concrete-measure ~last-sum)
                                (- ~concrete-measure ~last-sum)) ~error-bound))))
       {:new-vars sum-symbs}))

(defn oo
  "Generates measure constraints
   This is called on a single interval
   We generate p new sum variables
   For every pair of points we generate a fixed number of constraints"
  [[a b :as concrete-interval] concrete-measure symbolic-points dim]
  {:pre [(> (count symbolic-points) 1)]}
  (let [sum-symb-prefix (gensym "sum")
        a concrete-measure ;; remove
        sum-symbs (mapv #(symbol (str sum-symb-prefix %)) (range (count symbolic-points)))
        error-bound 10] ;FIXME-  hacked in concrete value
    (handle-oo (last sum-symbs) concrete-measure sum-symbs error-bound
      (mapv
        (fn [[s0 s1 :as symb-pair] sum-index]
          (let [[x y :as dim-interval] (mapv #(symbolic-project % dim) symb-pair)
                {new-vars :variables assertions :assertions volume :volume} (symbolic-volume symb-pair)
                sum-var (sum-symbs sum-index)
                ratio-volume (fn [denom a b] (symb* volume (symb-div denom (symb- b a))))
                sum-wha (fn [v] (if (zero? sum-index) v (symb+ v (sum-symbs (dec sum-index))))) ;FIXME
                or-term (template
                (or
                  (and (>= ~y ~x)
                    (or
                      (and (>= ~x ~b) (= ~sum-var ~(sum-wha 0.0)))
                      (and (> ~x ~a) (< ~x ~b) (> ~y ~b) (= ~sum-var ~(sum-wha (ratio-volume (symb- b x) a b))))
                      (and (>= ~x ~a) (< ~x ~b) (<= ~y ~b) (> ~y ~a) (= ~sum-var ~(sum-wha (ratio-volume (symb- y x) a b))))
                      (and (< ~x ~a) (<= ~y ~b) (> ~y ~a) (= ~sum-var ~(sum-wha (ratio-volume (symb- y a) a b))))
                      (and (<= ~x ~a) (<= ~y ~a) (= ~sum-var ~(sum-wha 0.0)))
                      (and (< ~x ~a) (> ~y ~b) (= ~sum-var ~(sum-wha (ratio-volume (symb- y a) a b))))))

                   (and (< ~y ~x)
                     (or
                        (and (>= ~y ~b) (= ~sum-var ~(sum-wha 0.0)))
                        (and (> ~y ~a) (< ~y ~b) (> ~x ~b) (= ~sum-var ~(sum-wha (ratio-volume (symb- b y) a b))))
                        (and (>= ~y ~a) (< ~y ~b) (<= ~x ~b) (> ~x ~a) (= ~sum-var ~(sum-wha (ratio-volume (symb- x y) a b))))
                        (and (< ~y ~a) (<= ~x ~b) (> ~x ~a) (= ~sum-var ~(sum-wha (ratio-volume (symb- x a) a b))))
                        (and (<= ~y ~a) (<= ~x ~a) (= ~sum-var ~(sum-wha 0.0)))
                        (and (< ~y ~a) (> ~x ~b) (= ~sum-var ~(sum-wha (ratio-volume (symb- x a) a b))))))))]
              {:assertions (conj assertions or-term) :new-vars new-vars}))
        (partition 2 1 symbolic-points) (range (count symbolic-points))))))

(defn normalisation-constraints
  "Points should be normalised"
  [points]
  (let [{volume :volume new-vars :variables assertions :assertions} ~(symbolic-volume points)]
    {:assertions (conj assertions (template (= ~volume 1.0))) :new-vars new-vars}))

(defn functional-constraints
  "y = f(x) constraints for points"
  [symb-f points]
  {:assertions (mapv #(template (= ~(symbolic-i-axis %) ~(symb-f (symbolic-d-axis %)))) points)})

(defn symb-sqr
  [x]
  (template (* ~x ~x )))

(defn rand-measure-constraints
  "Measure constraints
   For each of the n intervals"
  [n x symb-points]
  (let [intervals (rand-n-intervals x n)
        measures (map #(cond-expectation % x 1) intervals)
        constraints (map #(oo %1 %2 symb-points 1) intervals measures)
        f (count constraints)] ;FIXME - hardcoded dim in here
    constraints))

(defn unary-apply
  "Apply a unary function symb-f to some points"
  [symb-f points]
  (let [n-points 4
        symb-points (mapv #(vector (symbol (str "p" %))
                                  (symbol (str "d" %))
                                  (symbol (str "i" %)))
                         (range n-points))
        var-declares {:new-vars (flatten symb-points)}
        f-constraints
        (functional-constraints symb-f symb-points)
        n-constraint
        (normalisation-constraints symb-points)
        n-measure-constraints 3
        m-constraints (rand-measure-constraints n-measure-constraints points symb-points)
        m-constraints (apply (partial merge-with concat) m-constraints)
        all-constraints (merge-with concat m-constraints n-constraint f-constraints var-declares)
        all-declares (mapv #(template (declare-fun ~% () Real)) (:new-vars all-constraints))
        all-asserts (mapv #(template (assert ~%)) (:assertions all-constraints))
        ]
    {:asserts all-asserts :declares all-declares}))

;; ======
;; Example
(def x (->uniform-real 0 10 7))
(def points '[[p1 x1 y1 z1][p2 x2 y2 z2][p3 x3 y3 z3]])
(require '[fipp.edn :refer (pprint) :rename {pprint fipp}])
(def a (unary-apply symb-sqr (:points x)))
(fipp a)
