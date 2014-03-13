(ns ^{:doc "The actual interpreter"
      :author "Zenna Tavares"}
  relax.interpret
  (:require [clozen.helpers :as clzn])
  (import [java.io PushbackReader FileReader]))

(defn parse-clj [fname]
  (with-open [reader (PushbackReader. (FileReader. fname))]
    (loop [exprs []]
      (if-let [expr (read reader nil nil)]
        (recur (conj exprs expr))
        exprs))))

(defn eval-expr-seq
  "Load a clj file and interpret line by line using rules"
  [exprs]
  (clzn/loop-until-fn eval exprs))

(defn interpret-file
  "Load a file and interpret files in sequence"
  [fname]
  (eval-expr-seq (dbg (parse-clj fname))))

(defn repl
  "Very Simple Repl"
  []
  (println "===== Welcome to the construct REPL, type exit to exit ========")
  (loop [ip (read)]
    (case ip
      'exit (println 'Bye)
      (recur (read)))))

(def x (interpret-file "game-example.clj"))
