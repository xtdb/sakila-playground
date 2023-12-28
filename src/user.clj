(ns user
  (:require
   [xtdb.demo.web.server :refer [run-server]]
   [xtdb.demo.web.locator :as locator]
   xtdb.demo.db
   xtdb.demo.resources
   xtdb.demo.static-resources))

(def http-server
  (run-server {:port 3000 :join? false
               :locator locator/locate-resource}))
