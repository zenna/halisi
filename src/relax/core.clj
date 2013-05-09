(ns relax.core
  (:require [relax.render :as render])
  (:require [relax.query :as query])
  (:gen-class))

;; ======================================================================
(defn -main
  "main entry point."
  [& args]
  (query/main))