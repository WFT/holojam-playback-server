(ns playback-server.core
  (:require [playback-server.routes :as routes]
            [aleph.http :as http]
            [clojure.java.shell :refer [sh]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn msg-exit [status msg]
  ;; Borrowed from clojure/tools.cli example
  (println msg)
  (System/exit status))

(def cli-options
  ;; Adapted from clojure/tools.cli example
  [["-d" "--directory DIRECTORY" "Path to frame data directory."
    :default routes/frames-data-path]
   ["-p" "--port PORT" "Port number."
    :default 8080
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number (0, 65536)."]]
   ["-w" "--webbrowser" "Open a webbrowser to the main page (using Python)."]
   ["-h" "--help"]])

(defn usage [options-summary]
  ;; Adapted from clojure/tools.cli example
  (clojure.string/join
   \newline
   ["A tool to serve holojam line frames over the Internet using WebSockets"
    "or plain old HTTP." ""
    "Usage: lein run [options]" ""
    "Options:" options-summary ""
    "Check the README for more information."]))

(defn open-webbrowser [url]
  (try
    (let [proc (clojure.java.shell/sh
                "python" "-m" "webbrowser" url)
          success (zero? (:exit proc))]
      (when-not success
        (println "Couldn't open webbrowser.")
        (println (:err proc)))
      success)
    (catch java.io.IOException e
      (println "No python installation; couldn't open webbrowser.")
      false)))

(defn -main [& args]
  (let [{{:keys [help port webbrowser directory]} :options
         :keys [arguments errors summary]} (parse-opts args cli-options)]
    (cond
      help (msg-exit 0 (usage summary))
      webbrowser (open-webbrowser (str "http://localhost:" port))
      errors (msg-exit 1 (clojure.string/join \newline errors)))
    (println "Starting server on port" port)
    (binding [routes/frames-data-path directory]
      (http/start-server #'routes/app {:port port}))
    (shutdown-agents)))
