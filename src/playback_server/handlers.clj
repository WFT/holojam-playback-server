(ns playback-server.handlers
  (:require [playback-server.frames :as frames]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]
            [clj-struct.core :as clj-struct]
            [aleph.http :as http]
            [ring.util.response :as response]))

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

