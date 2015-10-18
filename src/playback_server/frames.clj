(ns playback-server.frames
  (:require [clojure.java.io :as io]
            [clj-struct.core :as clj-struct])
  (:import [org.apache.commons.io IOUtils]))

(defn get-frame-count
  "Given a path, find the number of frames in the path."
  [path]
  (with-open [num (io/input-stream (io/file path "frame_count"))]
    (let [buf (byte-array 4) ;; frame_count is of type UInt32
          n (.read num buf)]
      (first (clj-struct/unpack "<I" buf)))))

(defn get-frame
  "Get a byte-array of the specified frame data."
  [path n]
  (with-open [frame (io/input-stream
                     (io/file path (str "frame" n)))]
    (IOUtils/toByteArray frame)))
