;0. Make soft measure of convexity
; -- Two reason 1. We want
; -- We might imagine a function
; which takes anotehr function and some patterns
; a

; The objective is to have a test which might be easier to synthesise, and to look at the gradient
;1 Go to an optimisation method which allows search through arbitrary spaces.
;2 Find bottlenecks which are making code slow
;3 Noisify program, (automatically?)

(ns relax.query
  (:use relax.render)
  (:use relax.graphics)
  (:use clozen.helpers)
  (:use clozen.neldermead))

(defn blur
  "Add Gaussian blur to image"
  [img sigma])

(defn normal
  [x mean std]
  (* (reciprocal (Math/sqrt (* 2 Math/PI (sqr std))))
     (Math/exp (- (/ (sqr x) (* 2 (sqr std)))))))

(defn sum-gaussians
  "Summed log of gaussian likeluhood on every pixel"
  [proposal-img data-img]
  {:pre [(= (count proposal-img) (count data-img))]}
  (sum
    (for [i (range (count proposal-img))]
      (Math/log
        (normal (nth proposal-img i) (nth data-img i) 0.8)))))

; (defn boolean-compare
;   "Sum up 1s if matching 0 otherwise"
;   [proposal-img data-img]
;   {:pre [(= (count proposal-img) (count data-img))]}
;   (sum
;   (map
;     #(Math/abs (- (first %) (second %)))
;     (partition 2 (interleave proposal-img data-img)))))

(defn boolean-compare
  "Sum up 1s if matching 0 otherwise"
  [proposal-img data-img]
  ; (println "entering compare")
  (apply + (map bit-xor proposal-img data-img)))

(defn gen-cost-f
  "Generate a cost func wrt data (an img)"
  [data]
  (fn 
    [param-values]
    (let [flat-points (subvec param-values 0 (dec (count param-values))) ; first n-1 params are that of points
          ; pvar (println "PVALS" param-values)
          ; points (partition 2 param-values)
          points (convex-hull-gf (partition 2 param-values))
          ; nelder mead expects a flat vector need to unflatten
          
          ; pvar (println "rendering-points" points)
          rendered-img (poly-to-pixels points (:width data) (:height data))
          sigma (last points)
          quality (boolean-compare rendered-img (:data data))]
      quality)))

(defn inv-poly
  [data]
  (let [init-poly (vec (flatten (gen-unconstrained-poly (:width data) (:height data) 10)))
        ;pvar (println "init-poly" (count init-poly) init-poly)
        ]
  (println (read-line))
  (nelder-mead (gen-cost-f data)
               init-poly)))

(defn gen-test-data
  [width height]
  {:data (poly-to-pixels (gen-convex-poly width height 10) width height)
   :width width
   :height height})

; (def test-data {:data [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1]
;   :width 6 :height 6})

(defn main
  []
  (let [width 50
        height 50]
  (init-window width height "alpha")
  (init-gl)
  (inv-poly (gen-test-data width height))
  (print ";\n")
  (close-display)
  nil))