(ns ^{:doc "Axis Aligned Box (Orthorope) abstractions"
                :author "Zenna Tavares"}
  sigma.domains.box
  (:require [sigma.abstraction :refer :all]
            [clozen.geometry.box :refer [->Box]]))

(defrecord box-
  [axes boxes]

;; Rules
(rule -> (uniform-real id low-bound up-bound)  )
