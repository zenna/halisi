(ns ^{:doc "Parsing SVG files - Geometry"
      :author "Zenna Tavares"}
  relax.geometry.svg
  (:require [clozen.helpers :as clzn :refer :all])
  (:require [relax.geometry.common :refer :all])
  (:require [clojure.xml :as xml])
  (import [java.io FileInputStream]))

(defn d-attr-to-poly
  "Convert the d attribute of a path into a polygon.
   e.g. M 8.2142857,93.214286 40,85.714286 l -16.071429,-22.5 z
   
   Difficulties: 1) Weird scaling of coordinates
   2) Inkscape sometimes uses moveTo M (abs coord),
   sometimes line L (displacement coord)."
  [d-str]
  (let [d-parts (clojure.string/split d-str #" ")]
    (loop [d-parts-loop d-parts poly [] last-cmd nil]
      (cond
        (not (seq d-parts-loop)) poly

        ; It could be a command
        (= (clojure.string/lower-case (first d-parts-loop)) "m")
        (recur (next d-parts-loop) poly :move)

        (= (clojure.string/lower-case (first d-parts-loop)) "l")
        (recur (next d-parts-loop) poly :line)

        (= (clojure.string/lower-case (first d-parts-loop)) "z")
        poly

        ; Or a coordinate where last command was 'move'
        (= :move last-cmd)
        (recur 
          (next d-parts-loop)
          (conj poly 
                (mapv #(Double/parseDouble %)
                       (clojure.string/split (first d-parts-loop) #",")))
          last-cmd)

        ; Or a coordinate where last command was 'line'
        (= :line last-cmd)
        (recur (next d-parts-loop)
          (conj poly          ; Line command entails displacement,
                (add-vec      ; Hence do vector addition of last abs point
                  (last poly)
                  (mapv #(Double/parseDouble %)
                         (clojure.string/split (first d-parts-loop) #","))))
          last-cmd)

        :else
        (throw (Exception. "Unknown object in d attrs"))))))

(defmulti handle-xml
  "Dispatch on the tag type"
  (fn [node summary] (:tag node)))

;; Signal Handlers
; Extract the d attribute from a path
(defmethod handle-xml :path
  [node summary]
  (let [points-str (-> node :attrs :d)]
    (conj summary (d-attr-to-poly points-str))))

; Recurse on :contents of a :g node"
(defmethod handle-xml :g
  [node summary]
  (pass handle-xml summary (:content node)))

;"Most tags we don't care about - do nothing."
(defmethod handle-xml :default
  [node summary]
  summary)

(defn svg-xml-to-poly
  "Convert an svg"
  [match-xml]
  (let [summary []]
    (println "xml is " match-xml)
    (pass handle-xml summary (:content match-xml))))

(defn svg-file-to-poly
  [filename]
  (let [svg-xml (xml/parse (FileInputStream. filename))]
    (svg-xml-to-poly svg-xml)))
