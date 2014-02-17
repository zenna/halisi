(ns ^{:doc "Discrete Examples"
	    :author "Zenna Tavares"}
  relax.examples
  (:require [clozen.helpers :refer :all]))


;; Random examples
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

(def exp-line
  '(if (>= (+ x2 (* -1 x1)) 0)
        true
        false))

(def exp-linear
  '(or

    (and (>= x2 9) (<= x2 10))
    (and (>= x1 3) (>= x2 3) (<= x1 5) (<= x2 5))
    (and 
      (>= x1 0)
      (>= x2 0)
      (<= (+ x2 (* (- 1) x1)) 1)
      (<= (+ x1 (* 6 x2)) 15)
      (<= (+ (* 4 x1) (* (- 1) x2)) 10))))

(def exp-linear-overlap
  '(or

    (> (+ x2 (* -1 x1)) 0)
    (and (> x1 8) (> x2 2) (< x1 10) (< x2 4))))

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


(defn qual-example
  []
  {:vars '[x y]
   :pred
   '(or (and (< x 5) (> (+ y (* -1 x)) 2.5))
        (and (>= x 5) (< (+ y (* -0.5 x)) 2)
                      (> (+ y (* 0.5 x)) 10.5)))})

(def exp5
  '(and
    (or (> x1 1) (< x2 2) (> x1 3) (> x2 4))
    (or (> x1 5) (< x2 6) (> x1 7) (> x2 8))
    (or (> x1 9) (< x2 10) (> x1 11) (> x2 12))
    (or (> x1 13) (< x2 14) (> x1 15) (> x2 16))))

(def exp5
  '(and
    (or (> x1 1) (< x2 2))
    (or (> x1 5) (< x2 6))))

(def exp-abs
  '(and
    (> x1 2) (> x2 2) (< x1 8) (< x2 8)
    (or (> (+ x2 (* -1 x1)) 4) (> (+ x2 x1) 14))
    (or (> x2 7.5) (< x2 6.5))))
    ; (or (< (+ x2 (* -1 x1)) 0) (< (+ x2 x1) 10))))
    ; (or (< (+ x2 (* -1 x1)) 10) (< (+ x2 (* -1 x1)) 0))))

(def exp-abs
  '(and
    (> x1 2) (> x2 2) (< x1 8) (< x2 8)
    (or (> (+ x2 (* -1 x1)) 4) (> (+ x2 (* 3 x1)) 23))
    (or (> (+ x2 (* -0.1666 x1)) 6.666) (< x2 5))))
    ; (or (< (+ x2 (* -1 x1)) 0) (< (+ x2 x1) 10))))
    ; (or (< (+ x2 (* -1 x1)) 10) (< (+ x2 (* -1 x1)) 0))))

(def exp-testy-3d
  '(and
     (or
       (> (+ x2 (* -1 x1)) 4)
       (> (+ x2 (* 3 x1)) 23)
       (> (+ x1 (* -1 x3)) 0)
       (> (+ x3 (* 1 x2)) 23))
     (or
       (> (+ x1 (* 1 x2) (* 1 x3)) 5)
       (> (+ x2 (* 0.5 x3)) 6)
       (> (+ x1 (* -1 x2) (* -1 x3)) 5)
       (> (+ x3 (* 1 x2)) 0))))

(def exp-rand-3d
  `(~'and
     (~'or
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))
     (~'or
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
       (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
             (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))))

(def exp-rand-and-3d
  `(~'and
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
     (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
           (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))))

(def exp-rand-and-3d
  `(~'or
      (~'and
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
         (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
               (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

        (~'and
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

        (~'and
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

        (~'and
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
           (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                 (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))

            (~'and
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2)))
               (~'> (~'+ (~'* ~(dec (rand 2)) ~'x1) (~'* ~(dec (rand 2)) ~'x2)
                     (~'* ~(dec (rand 2)) ~'x3) (~'* ~(dec (rand 2)) ~'x4)) ~(dec (rand 2))))


        ))
    ; The problem with this is that we'll end up with a ccombinatorial explosion.

; (if (if (> x1 1)
;         true
;         (if (< x2 2)
;              true
;              false))
;     (if (if (> x1 5)
;             true
;             (if (< x2 6)
;                 true
;                 false))
;         true
;         false)
;     false)