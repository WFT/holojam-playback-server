
(ns playback-server.handlers
  (:require [playback-server.frames :as frames]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]
            [clj-struct.core :as clj-struct]
            [aleph.http :as http]
            [ring.util.response :as response]))

(defn send-frame-count
  "Sends the number of frames as little-endian UInt."
  [path stream]
  (println "Sending count...")
  (s/put! stream
          (byte-array
           (clj-struct/pack "<I" (frames/get-frame-count path)))))

(defn send-frame
  "Sends specified frame to the specified stream."
  [path frame stream]
  (if (or (pos? frame) (<= frame (frames/get-frame-count path)))
    (do
      (println "Sending frame" frame "...")
      (s/put! stream (frames/get-frame path frame)))
    (do
      (println "ERROR: Out of bounds frame number"
               frame "requested.")
      (s/put! stream (byte 0)))))

(defn handle-socket-message
  "Respond to a message on stream."
  [path stream ^bytes bmsg]
  (let [msg (String. bmsg)]
    (try
      (send-frame path
                  (Long/parseLong (clojure.string/trim msg))
                  stream)
      (catch NumberFormatException _
        (if (= (clojure.string/trim msg) "count?")
          (send-frame-count path stream)
          (do
            (println "ERROR: Improperly formatted message:" msg)
            (s/put! stream (byte 0))))))))

(defn read-frame-handler
  "Handle websocket connections and set up message handling functions."
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
        (partial handle-socket-message path conn)
        conn)))))

(defn read-frame-http-handler
  "Read a frame via vanilla HTTP."
  [path fnum]
  (let [frame (frames/get-frame path fnum)
             binary-stream (new java.io.ByteArrayInputStream frame)]
         (-> (response/response binary-stream)
             (response/content-type "application/octet-stream")
             (response/header "Content-Length" (count frame)))))
