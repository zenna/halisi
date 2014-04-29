(require '[clojure.math.combinatorics :as combo])

(defn form1 [a b c d e f]
  (or
  	(and a d)
  	(and a e)
  	(and a f)
  	b
  	(and c e)
  	(and c f)))

(defn form2 [a b c d e f]
  (or
  	(and d (not e))
  	(and f (not d) (not e))
  	(and a e)
  	(and (not c) (not a))))

(defn equiv-all-inputs?
  "Brute Force Check for Two Propositional Formulae"
  [f1 f2 n-inputs]
  (let [inputs (apply combo/cartesian-product (repeat n-inputs [true false]))]
  	(loop [inputs inputs]
  	  (cond 
  	  	(not (seq inputs))
  	  	true

  	  	(= (apply f1 (first inputs)) (apply f2 (first inputs)))
  	  	(recur (next inputs))

  	  	:else
  	  	(do 
  	  	  (println "differ on" (first inputs))
  	  	  false)))))

(equiv-all-inputs? form1 form2 6)