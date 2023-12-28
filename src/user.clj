(ns user
  (:require [ring.adapter.jetty :refer [run-jetty]]
            xtdb.demo.web.server
            xtdb.demo.db
            xtdb.demo.resources
            xtdb.demo.static-resources)
  (:import java.io.File
           java.time.Instant))

(def http-server
  (run-jetty
   #'xtdb.demo.web.server/handler
   {:port 3000
    :join? false}))
