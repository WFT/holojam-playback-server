(defproject playback-server "0.1.0-SNAPSHOT"
  :description "Serves holojam data to the web playback client."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [aleph "0.4.0"]
                 [manifold "0.1.0"]
                 [clj-struct "0.1.0"]]
  :main ^:skip-aot playback-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
