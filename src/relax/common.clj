(ns ^{:doc "Common"
      :author "Zenna Tavares"}
  relax.common)

;; An environment is a vector list of hashes [{}{}]
;Error
(defn error
  [& error-text]
  (println "ERROR" error-text)
  (throw (Throwable. )))

(defn tagged-list?
  "Lists are tagged by putting a symbol as first element"
  [exp tag]
  (if (coll? exp)
      (= (first exp) tag)
      false))

(defn tagged-vector?
  "Lists are tagged by putting a symbol as first element"
  [exp tag]
  (if (vector? exp)
      (= (first exp) tag)
      false))

; Application abstractions
(defn application? [exp] (coll? exp)) ;FIXME the test for application should not be coll?
(defn operator [exp] (first exp))
(defn operands [exp] (rest exp))
(defn no-operands? [ops] (empty? ops))
(defn first-operand [ops] (first ops))
(defn rest-operands [ops] (rest ops))