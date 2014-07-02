(defproject logkeeper "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://www.github.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.8"]
                 [selmer "0.6.7"]
                 [environ "0.5.0"]
                 [ring/ring-json "0.3.1"]
                 [org.ocpsoft.prettytime/prettytime "3.2.5.Final"]
                 [org.clojure/tools.logging "0.3.0"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [com.google.guava/guava "17.0"]]
  :source-paths ["src/main/clojure"]
  :resource-paths ["src/main/resources"]
  :test-paths ["src/test/clojure"]
  :global-vars {*warn-on-reflection* true}
  :main logkeeper.server
  :plugins [[lein-ring "0.8.9"] [lein-environ "0.5.0"]]
  :ring {:handler logkeeper.handler/app
         :init    logkeeper.handler/init}
  :profiles {:dev {:dependencies   [[ring-mock "0.1.5"]]
                   :resource-paths ["src/test/resources"]
                   :env            {:web-jetty-join true
                                    :web-jetty-threads 10
                                    :workdir           "/Users/sanshi/Documents/Work/WorkSpace/IntlliJSpace/Log/logkeeper/workdir"}}})
