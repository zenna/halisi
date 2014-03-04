;; NOTE! This project requires leiningen 2.1.0 or later.
(require 'leiningen.core.eval)
;;(println (leiningen.core.eval/get-os)) ;; try lein deps to see this

(def LWJGL-CLASSIFIER
  "Per os native code classifier"
  {:macosx "natives-osx"
   :linux "natives-linux"
   :windows "natives-windows"}) ;; TESTME

(defn lwjgl-classifier
  "Return the os-dependent lwjgl native-code classifier"
  []
  (let [os (leiningen.core.eval/get-os)]
    (get LWJGL-CLASSIFIER os)))

(defproject relax "0.1.0-SNAPSHOT"
  :description "Self relaxing programs"
  :url "https://github.com/rogerallen/hello_lwjgl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-marginalia "0.7.1"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lpsolve "5.5.2.0"]
                 [incanter "1.4.1"]
                 [criterium "0.4.1"]
                 [org.lwjgl.lwjgl/lwjgl "2.8.5"]
                 [org.lwjgl.lwjgl/lwjgl_util "2.8.5"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [org.clojure/data.priority-map "0.0.4"]
                 [org.lwjgl.lwjgl/lwjgl-platform "2.8.5" 
                  :classifier ~(lwjgl-classifier)
                  ]
                 [org.clojure/tools.trace "0.7.5"]
                 [fipp "0.4.1"]
                 [org.clojure/core.match "0.2.1"]]
  :main relax.core
  :jvm-opts [~(str "-Djava.library.path=/usr/local/lib:native/:"
                     (System/getProperty "java.library.path"))])
