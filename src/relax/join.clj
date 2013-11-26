(ns ^{:author "Zenna Tavares"
      :doc "Join"}
  ; "Join (abstract interpretation)
  ;  Joining takes two abstract objects in some domain and returns
  ;  another abstract object within the same domain."
  relax.join
  (:require [relax.common :refer :all]
            [clojure.walk :refer :all]))

; There are a number of important questions to resolve:
; 1. What precisely can I join

; 2. What should I join, what is the objective?
; Joining objects is to reduce the complexity.  Our goal is to
; reduce the program to a maneagable complexity, limiting the amount of 
; over approximation as much as possible

; 3. When joining two abstract objects say a and b, do I need to consider
; other objects, abstract or otherwise?
; Yes, other abstract objects may through the interactions in the program
; determine whether it is wise or not to join a and b.
; There are likely too many interactions to consider them all individually,
; so some alternative strategy is required.

; 4. How will joining, or deciding to join, be affected by other
; processes such as the refinement stage that happens afterwards.
; Well They are highly related.  We can view it as a two stage process, in which case,
; when joining we could either just assume that the following process
; will just improve whatever we've done already and so for all intents and purposes
; we can ignore it and just do our job.

; Or we can try to understand the implications,
; for the following stage.  At the most basic level this could be some rules we hard code 
; into the joiner which affect its decision makking based on observatiosn we've (as humans)
; seen.  Or it could learn

; More generally you could think of joinng and refining as two tool kits in the hands of the 
; interpreter and we should give it autonomy to do as it pleases.  This is
; much more complex and fits in line with stage 2.

; 4. How to join in practise?
; Well, my running

; Option 1. Somehow do normal evaluation until I reach a budget.
; How can I know if i've reached a budget?
; Once I've reached a budget how can I backtrack?
; Thi would interoduce an arbitrary order dependence.
;1

; So join will be an operation that happens in evaluation
; Most importantly in handle-combos.
; It will look deep into the nested structure and consider any joins
; joining def

; it needs to know what that will entail.
;; Complexiy in terms of reduction in dnf can be easily computed
;; Consider comparing joining (join x y z) with (join d e f)
;; if i do (join x y z) i know i'll get entities (join x y z)

; There are many questions of myopia nad ordering, for this example it's enough
; as a first approximation the joiner will do something like
; look at entities in its operands, decide whether to join one thing over another
; based on the reduction the number of terms
; and when you join these things.
; I'll construct a graph of all the abstract entities and use a louvain style decision
; saying basically if i joined these two

; Here's the thin

; For a cartesian product
; THe onbvious joins are the terms inside a disjunction
; Less obvio  

(defn join?
  [obj]
  (tagged-list? obj 'join-obj))

(defn make-join
  [args]
  `(~'join-obj ~(set args)))

; (def my-set [a b c][d e f][g h])
; (def joined ['(a d (joined g h))
;               ((joined a b) d)])

; (defn cartesian-product-x
;   "Find the cartesian product of"
;   [colls joined]


(defn join-substitute
  "Take an expression and join some terms in it
   joins is '[[a b c][d e]"
  [program joins]
  (postwalk-replace
    (apply merge
      (mapv
        (fn [join-set]
          (zipmap join-set
                  (repeat (count join-set) `(~'join ~@join-set))))
        joins))
    program))

; (comment
  