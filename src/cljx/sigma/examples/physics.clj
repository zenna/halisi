(ns ^{:doc "Quasi-physical simulation"
      :author "Zenna Tavares"}
  sigma.examples.physics)

(defn run-sim [state time-limit]
  (if (pos? time-limit)
      (run-sim (step state) (- time-limit delta-t))
      step))

(defn point-in-rect?
  "Is a point in a rectangle>"
  [[x y] [[lower-x upper-x] [lower-y upper-y]]]
  (and (<= x upper-x) (>= x lower-x)
       (<= y upper-y) (>= y lower-y)))

(defn intersect-circle-line?
  [circle line]

(defn intersect-hypersphere?
  "Do two hyperspheres intersect?
   They intersect if, and only if, the distance between their centers
   is between the sum and the difference of their radii.
   Avoids sqrt"
  [[c1 r1] [c2 r2]]
  (let [centre-diff (euclidean-dist-sqr c1 c2)]
    (and (<= (sqr (- r1 r2)) centre-diff)
         (<= centre-diff (sqr (+ r1 r2))))))

(defn intersect-circle-circle?
  [[[x1 y1] r1] [[x2 y2] r2]]
  (let [r-sqr (sqr (+ r1 r2))]
    (< (sqr ())

(defn enum-lines
  [[lower-x upper-x] [lower-y upper-y]]
  [[lower-x lower-y] [upper-x lower-y]
   [upper-x lower-y] [upper-x upper-y]
   [upper-x upper-y] [lower-x upper-y]
   [lower-x upper-y] [lower-x lower-y]])

(defn circle-rectangle-intersect
  [[centre r :as circle] rectangle]
  (let [lines (enum-lines rectangle)]
    (or (point-in-rect? centre rectangle)
        (intersect-circle-line? circle (lines 0))
        (intersect-circle-line? circle (lines 1))
        (intersect-circle-line? circle (lines 2))
        (intersect-circle-line? circle (lines 3)))))

(defn resolve-collision]
  [a b]
  

(defn step [[pos vel obstacles] delta-t]
  "Update the world with one delta-t time step"

  )

(comment
  (def init-pos [0.0 0.0])
  (def init-vel [1.0 1.0])
  (def obstales []))