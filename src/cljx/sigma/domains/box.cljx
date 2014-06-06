(ns ^{:doc "Axis Aligned Box (Orthorope) abstractions"
                :author "Zenna Tavares"}
  sigma.domains.box
  (:require [sigma.domains.abstract :refer [dist]]
            [clozen.geometry.box :refer [->Box]]))











;; ;; ========================= INSTA REPL
;; (require '[sigma.construct :refer :all]
;;          '   [clozen.helpers :as clzn]
;;             ' [clozen.iterator :refer [root realise update step end?]]
;;          '   [veneer.pattern.transformer :as transformer]
;;          '   [veneer.pattern.rule :refer [context-itr pat-rewrite]]
;;          '   [clojure.core.match :refer [match]]
;;          '   [fipp.edn :refer (pprint) :rename {pprint fipp}]
;;          '   [veneer.pattern.dsl-macros :refer [defrule]])


;; (defrule uniform
;;   "Define Rule"
;;   (-> (uniform x y) `(box [0 1])))

;; (def rules (concat std-rules [uniform]))

;; (:pre-context uniform)

;; (context-itr (:pre-context uniform) '(1 2 3))

;; (def eager-transformer (partial eager-transformer rules))

;; (def sigma-rewrite rewrite)


;; (sigma-rewrite '(uniform 0 1) eager-transformer)
