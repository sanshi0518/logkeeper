(ns logkeeper.home
  (:use logkeeper.render))

(defn home-page []
  (render-tpl "home.html"))
