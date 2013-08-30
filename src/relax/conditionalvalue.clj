(ns ^{:doc "Conditional Value"
      :author "Zenna Tavares"}
  relax.conditionalvalue
  (:use relax.common)
  (:use relax.multivalue)
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

(defn conditional-value?
  "A conditioned value has the form ('conditional multivalue)
   where each element of multivalue (possible-value) has the form
   ('possible-value value_i [condition_1, ..., condition_n])"
  [exp]
  (tagged-list? exp 'conditional-value))

(defn possible-value?
  [exp]
  (tagged-list? exp 'possible-value))

(defn all-possible-values
  [cond-val]
  (nth cond-val 1))

(defn all-conditions
  [val]
  "Return vector of all conditions"
  (vec (map #(nth % 2) (multivalues (all-possible-values val)))))

(defn all-values
  [val]
  "Return vector of all conditions"
  (vec (map #(nth % 1) (multivalues (all-possible-values val)))))

(defn value-conditions
  "Get the conditions of a conditioned value"
  [val]
  (nth val 2))

(defn merge-conditions
  [& value-conditions]
  (reduce #(vec (concat %1 %2)) value-conditions))

(defn conditional-value
  "Get the conditions of a conditioned value"
  [val]
  (nth val 1))

(defn apply-condition
  [val new-conditions]
  (list 'conditioned-value val new-conditions))

(defn make-conditional-value
  "Constructs a conditional value
   Takes a sequence of pairs of value and vector of conditions
   e.g. (make-conditional-value true [(< x 1)(> x 0)] false [(> x 1)]"
  [& args]
  {:pre [(even? (count args))]}
  (let [x (apply make-multivalue
                 (map (fn [x] `(~'possible-value ~@x)) (partition 2 args)))]
  `(~'conditional-value ~x)))

(defn arg-combinations
  [args]
  (apply combo/cartesian-product 
         (map #(if (conditional-value? %)
                   (multivalues (all-possible-values %))
                   [%])
              args)))

; TODO support nested multivalues
(defn handle-conditional
  "Allows a function to support conditional values
   If any arguments are conditional it applies the function to all
   combinations of their possible values.
   The outcome of any particular combination is conditioned on 
   conditions of any arguments that were used.

   If the function evaluates to a conditional value, possible values
   are expanded out and its conditions are included"
  [f & args]
  ; (println "args " args)
  (cond
    (not-any? conditional-value? args)
    [(apply f args)]

    :else
    (apply make-conditional-value
      (reduce concat
      (for [arg-list (arg-combinations args)
           :let [concrete-list (map #(if (possible-value? %) 
                                         (conditional-value %)
                                          %)
                                    arg-list)
                path-conditions (apply merge-conditions 
                                       (map value-conditions
                                            (filter possible-value? arg-list)))
                fx (apply f concrete-list)]]

        (if (conditional-value? fx)
            (reduce concat
              (map #(vector (conditional-value %)
                      (merge-conditions (value-conditions %) path-conditions))
                    (multivalues (all-possible-values fx))))

            [fx path-conditions]))))))