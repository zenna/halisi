(ns ^{:doc "Evaluation Environment"
      :author "Zenna Tavares"}
  relax.env)

; (defn set-first!
;   [frame values]
;   )

; (defn set-rest!
;   [frame values]
;   )

;; An environment is a vector list of hashes [{}{}]

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

(defn extend-environment
  "Extend a base env by a new frame that associates variables with values.
   Signal an error if the number of variables != number of values"
  [vars vals base-env]
  (if (= (count vars) (count vals))
      (conj base-env (make-frame vars vals))
      (if (< (count vars) (count vals))
        (error "Too many arguments supplied" vars vals)
        (error "Too few arguments supplied" vars vals))))

(defn lookup-variable-value
  "Lookup variable in environment:
   scan list of variables in first frame, if we find desired variable, then
   return corresponding value, otherwise search enclosing environment"
    [var env]
    (defn env-loop [env]
      (defn scan [vars vals]
        (cond
          (empty? vars)
          (env-loop (enclosing-environment env)))
        
          (eq? var (first vars))
          (first vals)

          :else
          (scan (rest vars) (rest vals)))
      (if (eq? env the-empty-environment)
          (error "Unbound variable" var)
          (let [frame (first-frame env)]
            (scan (frame-variables frame)
                  (frame-values frame)))))
    (env-loop env))

(defn define-variable!
  [var val env]
  "To define a variable, we search the first frame for a binding for the variable,
   and change the binding if it exists (just as in set-variable-value!).
   If no such binding exists, we adjoin one to the first frame."
   (add-binding-to-frame var val (first-frame frame) env))
   (if (nil?) ((first-frame env) var))
       (add-binding-to-frame! var val frame)
  (let [frame (first-frame env)
        scan  (fn [vars vals]
                (cond
                  (empty? vars)
                  (add-binding-to-frame! var val frame)

                  (eq? var (first vars))
                  (set-first! vals val)

                  :else
                  (recur (rest vars) (rest vals))))]
    (scan (frame-variables frame) (frame-values frame))))

(defn set-variable-value!
  "To set a variable to a new value in a specified environment, we scan for the
   variable, just as in lookup-variable-value, and change the corresponding value when we find it"
  [var val env]
  (defn env-loop [env]
    (defn scan [vars vals]
      (cond
        (empty? vars)
        (env-loop (enclosing-environment env)))467
    
        (eq? var (first vars))
        (set-car! vals val)

        :else
        (scan (rest vars) (rest vals)))
    (if (eq? env the-empty-environment)
        (error "Unbound variable: SET!" var)
        (let [frame (first-frame env)]
          (scan (frame-variables frame)
                (frame-values frame)))))
    (env-loop env))

(defn setup-environment
  []
  (let [initial-env (extend-environment (primitive-procedure-names)
                                        (primitive-procedure-objects)
                                        the-empty-environment)]
    (define-variable! ’true true initial-env)
    (define-variable! ’false false initial-env)
    initial-env))

(def the-global-environment (setup-environment))