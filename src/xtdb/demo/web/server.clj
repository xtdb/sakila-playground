(ns xtdb.demo.web.server
  (:require
   [ring.adapter.jetty :refer [run-jetty]]
   [xtdb.demo.web.handler :refer [make-full-ring-handler]]
   xtdb.demo.web.resource))

(defn make-handler [opts]
  (->
   (make-full-ring-handler opts)))

(defn run-server [opts]
  (run-jetty
   (make-handler opts) opts))
