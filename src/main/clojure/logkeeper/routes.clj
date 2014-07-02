(ns logkeeper.routes
  (:require [compojure.route :as route]
            [compojure.core :refer :all])
  (:use logkeeper.render
        logkeeper.logstash
        logkeeper.home))

(defn- not-found []
  {:status 404
   :body (render-tpl "not-found.html")})

(defroutes app-routes
           (GET "/" [] (home-page))
           (GET "/logstash/definition/home" [] (definition-home))
           (POST "/logstash/definition/new/:name" [] new-definition)
           (GET "/logstash/definition/detail/:name" [] definition-detail)
           (GET "/logstash/definition/delete/:name" [] delete-definition)
           (route/resources "/")
           (route/not-found (not-found)))