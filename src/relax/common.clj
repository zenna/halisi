(ns ^{:doc "Common"
      :author "Zenna Tavares"}
  relax.common)

;; An environment is a vector list of hashes [{}{}]
;Error
(defn error
  [& error-text]
  (println error-text)
  (throw (Throwable. )))

(defn tagged-list?
  "Lists are tagged by putting a symbol as first element"
  [exp tag]
  (if (list? exp)
      (= (first exp) tag)
      false))

(defn tagged-vector?
  "Lists are tagged by putting a symbol as first element"
  [exp tag]
  (if (vector? exp)
      (= (first exp) tag)
      false))

; Application abstractions
(defn application? [exp] (list? exp))
(defn operator [exp] (first exp))
(defn operands [exp] (rest exp))
(defn no-operands? [ops] (empty? ops))
(defn first-operand [ops] (first ops))
(defn rest-operands [ops] (rest ops))