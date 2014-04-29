(ns ^{:doc "Main, connects to the interpreter"}
    sigma.core
  (:require [sigma.interpret :refer :all]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string])
  (:gen-class))


(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-l" "--load LOAD" "Source file to load"]
   ;; If no required argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["σ is a probabilstic programming language."
        ""
        "This is the σ interpreter/compiler"
        ""
        "Usage: sigma filename.clj"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  repl     Open a repl"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (case (first arguments)
      "repl" (sigma-repl)
      (exit 1 (usage summary)))))