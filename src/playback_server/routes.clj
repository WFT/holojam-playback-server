(ns playback-server.routes
  (:require [playback-server.handlers :refer :all]
            [playback-server.frames :as frames]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer
             [wrap-defaults api-defaults]]
            [ring.middleware.resource :refer
             [wrap-resource]]
            [ring.util.response :as response]))

(def ^:dynamic frames-data-path
  "The directory from which to pull frame data."
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
  "Webapp encapsulation."
  (-> #'app-routes
      (wrap-resource "unity")
      (wrap-defaults api-defaults)))

