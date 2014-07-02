(ns logkeeper.render
  (:require [selmer.parser :as parser]
            [clojure.string :as s]
            [ring.util.response :refer [content-type response]]
            [compojure.response :refer [Renderable]]))

(def tpl-path "templates/")

(defn render-tpl [template & [params]]
  (parser/render-file (str tpl-path template) params))