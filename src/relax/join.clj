(ns ^{:doc "Join"
      :author "Zenna Tavares"}
  relax.join
  (:use relax.common)
  (:use clojure.walk))

;1 
(defn join?
  [obj]
  (tagged-list? obj 'join-obj))

(defn make-join
  [args]
  `(~'join-obj ~(set args)))

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

(def prog
  '(and (>= x0 0.9) (<= x0 1.1) (>= y0 0.9) (<= y0 1.1) (>= x2 8.9) (<= x2 9.1) (>= y2 8.9) (<= y2 9.1) (>= (+ x1 (* -1 x0)) 0) (<= (+ x1 (* -1 x0)) 5) (>= (+ x2 (* -1 x1)) 0) (<= (+ x2 (* -1 x1)) 5) (or (<= x0 2) (>= x0 5) (<= y0 5) (>= y0 7)) (or (<= x0 5) (>= x0 8) (<= y0 0) (>= y0 3)) (or (<= x1 2) (>= x1 5) (<= y1 5) (>= y1 7)) (or (<= x1 5) (>= x1 8) (<= y1 0) (>= y1 3)) (or (<= x2 2) (>= x2 5) (<= y2 5) (>= y2 7)) (or (<= x2 5) (>= x2 8) (<= y2 0) (>= y2 3))))