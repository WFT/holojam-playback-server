(ns playback-server.core
  (:require [playback-server.routes :as routes]
            [aleph.http :as http]
            [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "8080"))]
    (println "Starting server on port" port)
    (future
      (Thread/sleep 3000)
      (try
        (let [proc (clojure.java.shell/sh
                    "python" "-m" "webbrowser" (str "localhost:" port))]
          (when-not (= 0 (:exit proc))
            (println "Couldn't open webbrowser.")
            (println (:err proc))))
        (catch java.io.IOException e
          (println "No python installation; couldn't open webbrowser."))))
    (binding [routes/frames-data-path (or (second args) routes/frames-data-path)]
      (http/start-server #'routes/app {:port port}))
    (shutdown-agents)))
