(ns ^{:doc "Run experiments"
      :author "Zenna Tavares"}
  relax.run.experiments
  (:require [relax.experiments :refer :all])
  (:require [taoensso.timbre :as timbre
                      :refer (trace debug info warn error fatal spy with-log-level)]))

; (timbre/set-level! :warn)

(defn -main[]
  (run-all-experiments))