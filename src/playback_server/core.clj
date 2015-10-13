(ns playback-server.core
  (:require [playback-server.frames :as frames]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer
             [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer
             [wrap-resource]]
            [ring.util.response :as response]
            [aleph.http :as http]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]
            [clj-struct.core :as clj-struct]            
            [clojure.java.shell :refer [sh]])
  (:gen-class))

(def ^:dynamic frames-data-path
  "recorded_frames")

(defn read-frame-handler
  [path request]
  (d/let-flow
   [conn (d/catch
             (http/websocket-connection request)
             (fn [_] nil))]
   (if-not conn
     (do
       (println "ERROR: Expected a WebSocket connection")
       "<h1>Expected a WebSocket connection.</h1>")

     (do
       (println "New WebSocket connection...")
       (s/consume
        (fn [msg]
          (let [msg (String. msg)]
            (try
              (let [n (Long/parseLong (clojure.string/trim msg))]
                (if (or (< 0 n) (<= n (frames/get-frame-count path)))
                  (do
                    (println "Sending frame" n "...")
                    (s/put! conn (frames/get-frame path n)))
                  (do
                    (println "ERROR: Out of bounds frame number"
                             n "requested.")
                    (s/put! conn (byte 0)))))
              (catch NumberFormatException e
                (if (= (clojure.string/trim msg) "count?")
                  (do
                    (println "Sending count...")
                    (s/put! conn
                            (byte-array
                             (clj-struct/pack "<I" (frames/get-frame-count path)))))
                  (do
                    (println "ERROR: Improperly formatted message."
                             "Message:" msg
                             "Error:" e)
                    (s/put! conn (byte 0))))))))
        conn)))))

(defn read-frame-http-handler
  [path fnum]
  (let [frame (frames/get-frame path fnum)
             binary-stream (new java.io.ByteArrayInputStream frame)]
         (-> (response/response binary-stream)
             (response/content-type "application/octet-stream")
             (response/header "Content-Length" (count frame)))))

(defroutes app-routes
  (GET "/frames" request
       (read-frame-handler frames-data-path request))
  (GET "/frames/count" []
       (frames/get-frame-count frames-data-path))
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
