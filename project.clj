;; NOTE! This project requires leiningen 2.1.0 or later.
(require 'leiningen.core.eval)
;;(println (leiningen.core.eval/get-os)) ;; try lein deps to see this

(defproject sigma "0.1.0-SNAPSHOT"
  :description "Self sigmaing programs"
  :url "https://github.com/rogerallen/hello_lwjgl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[com.keminglabs/cljx "0.3.2"]
            [lein-cljsbuild "1.0.2"]]
  :dependencies [[org.clojure/clojurescript "0.0-2030"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [org.clojure/tools.trace "0.7.5"]
                 [fipp "0.4.1"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [net.mikera/vectorz-clj "0.22.0"]
                 [backtick "0.3.0"]
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
  :hooks [cljx.hooks]
  :main sigma.core)
