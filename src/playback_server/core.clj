(ns playback-server.core
  (:require [playback-server.handlers :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer
             [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer
             [wrap-resource]]
            [ring.util.response :as response]
            [aleph.http :as http]
            [clojure.java.shell :refer [sh]]
            [playback-server.frames :as frames])
  (:gen-class))

(def ^:dynamic frames-data-path
  "recorded_frames")

(defroutes app-routes
  (GET "/frames" request
       (read-frame-handler frames-data-path request))
  (GET "/frames/count" []
       (str (frames/get-frame-count frames-data-path)))
  (GET "/frames/:fnum{[0-9]+}" [fnum]
       (read-frame-http-handler frames-data-path fnum))
  (GET "/" []
       (response/redirect "/index.html"))
  (route/not-found "404 Not Found"))

(def app
  (-> #'app-routes
      (wrap-resource "unity")
      (wrap-defaults api-defaults)))

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
    (binding [frames-data-path (or (second args) frames-data-path)]
      (http/start-server #'app {:port port}))
    (shutdown-agents)))
