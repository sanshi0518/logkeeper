(ns logkeeper.middleware
  (:use clojure.tools.logging
        logkeeper.render))

(defn wrap-error-handler [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (error e "Process request failed")
        {:status 500
         :body (render-tpl "error.html" {:error (or (.getMessage e) e)})}))))