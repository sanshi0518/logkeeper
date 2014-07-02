(ns logkeeper.handler
  (:require [compojure.handler :as handler]
            [selmer.parser :as parser]
            [selmer.filters :as filters]
            [selmer.middleware :refer [wrap-error-page]])
  (:use [ring.middleware.json :only [wrap-json-params]]
        environ.core
        clojure.tools.logging
        logkeeper.middleware
        logkeeper.routes
        logkeeper.util))

(defn init []
  (let [subdirs ["definition" "template" "input"]]
    (doseq [subdir subdirs]
      (-> (env :workdir)
          (str separator subdir)
          (mkdirs))))
  (parser/cache-off!)
  (filters/add-filter! :trim #(.trim %))
  (filters/add-filter! :nbsp #(.replace % " " "&nbsp;")))

(def app
  (handler/site (-> app-routes
                    wrap-json-params
                    (wrap-error-page (env :dev))
                    wrap-error-handler)))