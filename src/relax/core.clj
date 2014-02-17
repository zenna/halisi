(ns relax.core
  (:require [relax.geometry.render :as render]
  			[relax.query :as query])
  (:gen-class))

;; ======================================================================
(defn -main
  "main entry point."
  [& args]
  (query/main))