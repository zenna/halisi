(ns ^{:doc "Run experiments"
      :author "Zenna Tavares"}
  sigma.run.experiments
  (:require [sigma.experiments :refer :all])
  (:require [taoensso.timbre :as timbre
                      :refer (trace debug info warn error fatal spy with-log-level)]))

; (timbre/set-level! :warn)

(defn -main[]
  (run-all-experiments))