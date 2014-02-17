(ns ^{:doc "Parsing SVG files - Geometry"
      :author "Zenna Tavares"}
  relax.geometry.svg
  (:require [clozen.helpers :as clzn :refer :all])
  (:require [relax.geometry.common :refer :all])
  (:require [clojure.xml :as xml])
  (import [java.io FileInputStream]))

(defn parse-xml-coords
  "String coordinate to vector of doubles"
  [coords-str]
  (mapv #(Double/parseDouble %)
         (clojure.string/split coords-str #",")))

;; X and Y seem to be distance from TOP LEFT. therefore there should be nothing in y greater
;; than 150
;; Lower 
(defn d-attr-to-points
  "Convert the d attribute of a path into a polygon.
   Only works with absolute coordinates.

   Note there may be a weird ordering of vertices.  By weird, I think
   it may be the case that different obstacles can have different orderings
   i.e. CW and CCW.
   
   Also note, Inkscape coordinates are a bit strange."
  [d-str]
  (let [d-parts (clojure.string/split d-str #" ")]
    (mapv parse-xml-coords (-> d-parts pop next))))

(defmulti handle-xml
  "Dispatch on the tag type"
  (fn [node summary] (:tag node)))

;; Signal Handlers
; Extract the d attribute from a path
(defmethod handle-xml :path
  [node summary]
  (let [{id :id points-str :d} (-> node :attrs)
        points (d-attr-to-points points-str)]
    (conj summary {:id id :type :path :data points}))) 

; Extract the d attribute from a path
(defmethod handle-xml :rect
  [node summary]
  (let [{w :width h :height x :x y :y id :id}
        (-> node :attrs)
        w (Double/parseDouble w)
        h (Double/parseDouble h)
        x (Double/parseDouble x)
        y (Double/parseDouble y)]
    (conj summary 
          {:id id :type :box :data [[x (+ x w)][y (+ y h)]]})))

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
    (pass handle-xml summary (:content match-xml))))

(defn svg-file-to-poly
  [filename]
  (let [svg-xml (xml/parse (FileInputStream. filename))]
    (svg-xml-to-poly svg-xml)))

(comment
  (def obstacles (svg-file-to-poly "scene5.svg"))
  (require '[relax.geometry.convex :refer :all])
  (mapv convex? obstacles))