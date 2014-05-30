(ns ^{:doc "Discrete Examples"
	    :author "Zenna Tavares"}
  sigma.examples
  (:require [clozen.helpers :refer :all]))

(use 'clozen.helpers)

;; How to go from this to a closed form solution? 
(defn game [player]
  (let [probabilities {'player1 (/ 1 3) 'player2 0.25}
        other-player #(if (= 'player1 %) 'player2 'player1)]
    (if (flip (probabilities player))
        player
        (recur (other-player player)))))

(game 'player1)

(defn disease-burger-model
  "Doctors find that people with Kreuzfeld-Jacob disease (KJ) almost
  invariably ate hamburgers, thus p(Hamburger Eater|KJ )= 0.9.
  The probability of an individual having KJ is currently rather low, about
  one in 100,000.  Half of the population eats burgers"
  []
  (let [has-disease (flip (/ 1 100000))
        eats-burgers (if (has-disease) (flip 0.9) (flip 0.5))]
    [eats-burgers has-disease]))

(defn disease-burger-query
  "what is the probability that a hamburger eater will have
   Kreuzfeld-Jacob disease?"
  []
  (let [joint-dist (disease-burger-model)
        eats-burgers (first joiint-dist)
        has-disease (second joint-dist)]
    (probability
      (condition disease-burger-model #(and (not has-disease) eats-burgers)))))

;; Geometric Distributions
(defn geometric
  "Geometric Distribution in a different form"
  [p]
  (if (flip p)
      0
      (+ 1 (geometric p))))

(defn geometric
  "Geometric Distribution in a different form"
  [n p]
  (if (flip p)
      n
      (geometric (+ 1 n) p)))