(ns ^{:doc "Evaluation Environment"
      :author "Zenna Tavares"}
  sigma.env
  (:use sigma.common))

; Environment abstractions
(defn enclosing-env [env] (rest env))
(defn first-frame [env] (first env))
(def the-empty-environment (atom []))
(defn make-frame [variables values]
  (zipmap variables values))
(defn frame-variables [frame] (keys frame))
(defn frame-values [frame] (vals frame))
(defn add-binding-to-frame! [var val frame-pos env]
  (swap! env #(assoc-in % [frame-pos var] val)))
(defn nth-frame
  "Return the nth frame"
  [n env]
  (nth @env n))
; (defn get-frame-var
;   [frame var]

(defn extend-environment
  "Extend a base env by a new frame that associates variables with values.
   Signal an error if the number of variables != number of values"
  [vars vals base-env]
  (if (= (count vars) (count vals))
      (atom (conj @base-env (make-frame vars vals)))
      (if (< (count vars) (count vals))
        (error "Too many arguments supplied" vars vals)
        (error "Too few arguments supplied" vars vals))))


(defn define-variable!
  [var val env]
  "To define a variable, we search the first frame for a binding for the variable,
   and change the binding if it exists (just as in set-variable-value!).
   If no such binding exists, we adjoin one to the first frame."
   (add-binding-to-frame! var val 0 env))

(defn lookup-variable-value
  "Lookup variable in environment:
   scan list of variables in first frame, if we find desired variable, then
   return corresponding value, otherwise search enclosing environment"
    [var env]
    ; (println "env is" env)
    (loop [frame-pos 0]
      (let [curr-frame (nth-frame frame-pos env)]
        (cond
          (not (nil? (curr-frame var)))
          (curr-frame var)

          (= frame-pos (dec (count @env)))
          (error "Unbound variable: SET!" var)

          :else
          (recur (inc frame-pos))))))

(defn set-variable-value!
  "To set a variable to a new value in a specified environment, we scan for the
   variable, just as in lookup-variable-value, and change the corresponding value when we find it"
  [var val env]
  (loop [frame-pos 0]
    (cond
      (= frame-pos (count @env))
      (error "Unbound variable: SET!" var)

      (nil? ((nth-frame frame-pos env) var))
      (recur (inc frame-pos))

      :else
      (add-binding-to-frame! var val frame-pos env))))