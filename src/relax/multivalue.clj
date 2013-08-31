(ns ^{:doc "Multivalues"
      :author "Zenna Tavares"}
  relax.multivalue
  (:use relax.common)
  (:use clozen.helpers)
  (:require [clojure.math.combinatorics :as combo]))

;; Mutlivalue abstractions
(defn make-multivalue
  [& values]
  "Make a multivalue"
  (list 'multivalue (vec values)))

(defn multivalue? [val]
  "Construct a multivalue: a compound value where the variable has many possible values"
  (tagged-list? val 'multivalue))

(defn multivalues [val]
  (if (multivalue? val)
      (nth val 1)
      [val]))

(defn make-merge-multivalue
  "Make a multivalue but if any of the values are already multivalues then merge them in
  (as opposed to creating nested multivalues)"
  [& values]
  (if (not-any? multivalue? values)
      (apply make-multivalue values)
      (reduce 
        #(cond
          (and (multivalue? %1) (multivalue? %2))
          (list 'multivalue (vec (concat (multivalues %1) (multivalues %2))))

          (multivalue? %1)
          (list 'multivalue (conj (multivalues %1) %2))

          (multivalue? %2)
          (list 'multivalue (conj (multivalues %2) %1))

          :else
          (make-multivalue %1 %2))
        values)))

; TODO support nested multivalues
(defn multify
  "Multify takes a function that does not support multivalues
   and returns one that does"
  [f & args]
  (cond
    (not-any? multivalue? args)
    (apply f args)

    :else
    (apply make-multivalue
      (map #(apply f %) 
            (apply combo/cartesian-product (map multivalues args))))))

(defn multify-apply
  [f args]
  (cond
    (not-any? multivalue? args)
    (apply f args)

    :else
    (apply make-multivalue
      (map #(apply f %) 
            (apply combo/cartesian-product (map multivalues args))))))
