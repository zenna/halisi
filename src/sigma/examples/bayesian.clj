(ns ^{:doc "Textbook Bayesian Inference Questions"
      :author "Zenna Tavares"}
  sigma.bayesian
  (:require [clozen.helpers :refer :all]))

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
   Kreuzfeld-Jacobdisease?"
  []
  (let [joint-dist (disease-burger-model)
        eats-burgers (first joiint-dist)
        has-disease (second joint-dist)]
    (probability
      (condition disease-burger-model #(and (not has-disease) eats-burgers)))))

;; Geometric Distributions
(defn geometric
  [p]
  (if (flip p)
      0
      (+ 1 (geometric p))))

(defn geometric
  [n p]
  (if (flip p)
      n
      (geometric (+ 1 n) p)))

precondition is that n is a non-negative integer.

;; Memoisation
; I can't see how memoisation would be a good idea.
; The problem its trying to solve is say
(define strength (mem (lambda (person) (gaussian 0 1))))

(define lazy (lambda (person) (flip 0.25)))

(define (pulling person)
  (if (lazy person) (/ (strength person) 2) (strength person)))

(define (total-pulling team)
  (sum (map pulling team)))

(define (winner team1 team2) (if (< (total-pulling team1) (total-pulling team2)) team2 team1))

(list "Tournament results:"
      (winner '(alice bob) '(sue tom))
      (winner '(alice bob) '(sue tom))
      (winner '(alice sue) '(bob tom))
      (winner '(alice sue) '(bob tom))
      (winner '(alice tom) '(bob sue))
      (winner '(alice tom) '(bob sue)))

(defn strength
  [name]
  ;;

(define (winner team1 team2) (if (< (total-pulling team1) (total-pulling team2)) team2 team1))

(define (sequences first-val)
  (mh-query
   1000 10
   (define prob (if (flip) 0.2 0.7))
   (define (myflip) (flip prob))
   (define s (repeat 10 myflip))
   (second s)
   (equal? (first s) first-val)))

(multiviz 
 (hist (sequences true)  "second if first is true")
 (hist (sequences false) "second if first is false"))