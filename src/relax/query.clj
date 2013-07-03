;0. Make soft measure of convexity
; -- Two reason 1. We want
; -- We might imagine a function
; which takes anotehr function and some patterns
; a

; The objective is to have a test which might be easier to synthesise, and to look at the gradient
;1 Go to an optimisation method which allows search through arbitrary spaces.
;2 Find bottlenecks which are making code slow
;3 Noisify program, (automatically?)
;1 Go to an optimisation method which allows search through arbitrary spaces.

; in church you defoinn e generative modend we want to condition on some aspects of the programl
; and condition on variables in that program
; WHat I am ataking about is 
; The high level concept is thawe have some program and we want to pmake statements about the program
; things which are true about the program.
; What does this mean throughthink of an imperative program that sorts lists
; we might want to condition on the output and observe the input
; the thinkg is that the we can easily ask impossibl questoins such as cpndition on an unsorrted output
; But what are we really going when we conditon on some output\
; def a (sort b)
; assume a is [1 2 3 4 5], what's b.  Well b could be any combination of [1 2 3 4 5]
; in order to sample we need some probability distribution.  OR mor generally some bias of one over the others.
; Providing a distribution allows you to take a single sample in a meaningful way, on the other hand there are at least
; some example where we can answer the question more abstractly from a logical defintion of the program.

; So we might want something of the form, where it tells us that if we condition on the output being [1 2 3 4 5],
; an ask about the input, we know the input will be any combingatin of the input, this implies our resutls must be returned in some 
; language.  This is a noble goal, most surely.  But what itwe is precisely.  What we are saying, is that we can condition on the
; input or output of a program and find logical constraints on the input.  Our answers are declarative.

; Another example, conider inverse graphics, we can condition on some output image.  The difference herei sthat there is 
; no language in which to define the input in a deductive way.  Thi would require a more indepth knowledge of logic programming
; and constraint programming to see how these things work.

; A more immediate goal could be taking an arbitrary program and having holes I wish to fill.

; Put noise on points, do inference over these points to

; PROBLEMS

; 1. Getting stuck in local minima
; 2. using weird rendering of complex shapes
; 3. Using wrong nuber of points
; 4. plateaus, if there is no overlap then it wont have any gradient
; 5. Bad stopping criterion in nelder-mead.
; More fundamentally generative model is poor!
; Leslie  

(ns relax.query
  (:use relax.render)
  (:use relax.graphics)
  (:use clozen.helpers)
  (:use clozen.neldermead)
  (:require [incanter.distributions :as dist]))

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
          points (vec (partition 2 param-values))
          ; pvar (println "convexity" (soft-convexity points) (convexity-measure points))
          ; points (convex-hull-gf (partition 2 param-values))
          ; nelder mead expects a flat vector need to unflatten
          
          ; pvar (println "rendering-points" points)
          rendered-img (poly-to-pixels points (:width data) (:height data))
          sigma (last points)
          quality (boolean-compare rendered-img (:data data))
          convexity (convexity-measure points)]
      (+ quality convexity (soft-convexity points)))))

(defn max-poly-convexity
  "Maximise the convexity of a polygon"
  [poly]
  (let [cost-f (fn [flat-poly]
                  (let [unflat-poly (vec (partition 2 flat-poly))]
                    ; (draw-poly-standalone unflat-poly)
                    (convexity-measure unflat-poly)))
        {conv-poly :vertex} (nelder-mead-noisy cost-f (vec (flatten poly)) 3000)]
        conv-poly))

; NOTEST
(defn add-normal-noise
  "adds noise in both x y to a polygon"
  [poly std]
  (mapv #(dist/draw (dist/normal-distribution % std)) poly))

; ; NOTEST
; (defn noisy-cost-f
;   [params]
;   (let [flat-poly (subvec params 0 (dec (count params)))
;         unflat-poly (vec (partition 2 flat-poly))
;         std (last params)
;         pvar (println "sd" std)
;         scores (repeatedly 10
;           (fn []
;             (let [noisy-poly (mapv #(add-normal-noise % std) unflat-poly)]
;               (draw-poly-standalone noisy-poly)
;               (convexity-measure noisy-poly))))]
;     (println (* 1.0 (mean scoreFs)))
;     (mean scores)))

; NOTEST
(defn noisy-cost-f
  [params]
  (let [[flat-poly stds] (vec (split-at (* 2 (/ (count params) 3)) params))
        unflat-poly (vec (partition 2 flat-poly))
        ; pvar (println "s" stds)
        ; pvar (println "vl" (nth params 7))
        scores (repeatedly 1000
          (fn []
            (let [noisy-poly (mapv #(add-normal-noise %1 %2) unflat-poly stds)]
              ; (draw-poly-standalone noisy-poly)
              (convexity-measure noisy-poly))))]
    (draw-poly-standalone unflat-poly)
    ; (println (double (mean scores)) (count params))
    (mean scores)))

; ; NOTEST
; (defn max-poly-convexity-noisy
;   "Maximise the convexity of a polygon"
;   [poly]
;   (let [std 5
;         {conv-poly :vertex} (nelder-mead noisy-cost-f (conj (vec (flatten poly)) std))]
;         conv-poly))

(defn max-poly-convexity-noisy
  "Maximise the convexity of a polygon"
  [poly]
  (let [stds (vec (repeatedly (count poly) #(* (rand) 35)))
        ; stds (assoc stds 3 10)
        pvar (println stds)
        init-data (vec (flatten (conj poly stds)))
        ; pvar (println "start-point" init-data)
        {conv-poly :vertex} (nelder-mead noisy-cost-f init-data 500)]
        conv-poly))


(defn inv-poly
  [data]
  (let [init-poly (vec (flatten (gen-unconstrained-poly
                                (:width data)
                                (:height data) 5)))
        ; init-poly (vec [[0.0 0.0] [50.0 0.0] [50.0 50.0] [25.0 45.0] [0.0 50.0]])

        init-poly (max-poly-convexity-noisy init-poly)
        ; pvar (println "init-poly" (count init-poly) init-poly)
        ; pvar (R "ip" init-poly)
        ]
  (println "Ready to do inverse graphics, press key to continue" )
  (println (read-line))
  ; ))
  (nelder-mead-noisy (gen-cost-f data)
               init-poly
               1000)))

(defn gen-test-data
  [width height]
  {:data (poly-to-pixels (gen-convex-poly width height  5) width height)
   :width width
   :height height})

(defn main
  []
  (let [width 200
        height 200]
  (init-window width height "alpha")
  (init-gl)
  (let [test-data (gen-test-data width height)]
    (println "Showing Test Data" (read-line))
    (inv-poly test-data) 
    (print ";\n")
    (close-display)
    nil)))

; (defn main
;   []
;   (let [width 200
;         height 200
;         init-poly (vec (flatten (gen-unconstrained-poly
;                                         width
;                                         height 5)))]
;   (init-window width height "alpha")
;   (init-gl)
;   (println (read-line))
;   (max-poly-convexity init-poly)
;   (print ";\n")
;   (close-display)
;   nil))