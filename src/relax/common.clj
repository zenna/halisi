(ns ^{:doc "Common"
      :author "Zenna Tavares"}
  relax.common
  (:use clojure.java.io))

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

;; Debug
(defn dnf-to-sets
  [dnf]
  (map (comp set (fn [conj] (map second conj))) dnf))

(defn filter-terms
  [dnf-sets must-contain]
  (let [contain-funcs
        (map #(fn [dnf-set] (contains? dnf-set %)) must-contain)]
    (filter (fn [dnf-set]
              ; Check that every (f dnf-set is true)
              (every? #(true? (% dnf-set)) contain-funcs))
       dnf-sets)))

;; IO
(defn matrix-to-file
  [matrix fname]
  (doall
    (for [vector matrix]
      (with-open [wrtr (writer fname :append true)]
        (.write wrtr  (str (clojure.string/join " " vector)))
        (.write wrtr "\n")))))

(defn samples-to-file
  "Writes a set of samples to file
   Creates two files, in one each line denotes a single sample
   In the other each line denotes a variable"
  [fname samples]
  (let [num-dim (count (first samples))
          re-samples
        (for [i (range num-dim)]
          (map #(nth % i) samples))]
    (doall
      (for [sample samples]
        (with-open [wrtr (writer (str fname "-lines") :append true)]
          (.write wrtr  (str (clojure.string/join " " sample)))
                    ; (.write wrtr (apply str (doall sample)))

          (.write wrtr "\n"))))
    (doall
      (for [dim re-samples]
        (with-open [wrtr (writer fname :append true)]
          (.write wrtr (str (clojure.string/join " " dim) "\n")))))))