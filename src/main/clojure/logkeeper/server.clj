(ns logkeeper.server
  (:gen-class :main true)
  (:use environ.core
        clojure.tools.logging
        ring.adapter.jetty
        logkeeper.handler))

(defn get-port [args]
  (let [port (first args)]
    (if (number? port) port 3078)))

(defn -main [& args]
  (let [max-threads (env :web-jetty-threads 30)
        port (get-port args)]
    (info "Starting logkeeper server at port " port ".")
    (init)
    (let [server (run-jetty app {:port port
                                 :max-threads max-threads
                                 :join? (env :web-jetty-join false)})]
      (info "Starting logkeeper server successfully.")
      server)))

