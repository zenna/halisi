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

(defproject sigma "0.1.0-SNAPSHOT"
  :description "Self sigmaing programs"
  :url "https://github.com/rogerallen/hello_lwjgl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-marginalia "0.7.1"]
            [com.keminglabs/cljx "0.3.2"]
            [lein-cljsbuild "1.0.2"]]
  :dependencies [[org.clojure/clojurescript "0.0-2030"]
                 [org.clojure/clojure "1.6.0"]
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
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [net.mikera/vectorz-clj "0.22.0"]
                 [clozen "0.1.0-SNAPSHOT"]
                 [veneer "0.1.0-SNAPSHOT"]]
  :source-paths ["src/clj" "target/classes"]
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}]}

  :cljsbuild {:builds
              {:dev {:source-paths ["src/clj" "target/classes"]
                     :compiler {:output-to "target/main.js"
                                :optimizations :whitespace
                                :pretty-print true}}}}
;;   :hooks [cljx.hooks]
  :main sigma.core
  :jvm-opts [~(str "-Djava.library.path=/usr/local/lib:native/:"
                     (System/getProperty "java.library.path"))])
