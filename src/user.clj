(ns user
  (:require
   [xtdb.demo.web.server :refer [run-server]]
   [xtdb.demo.web.locator :as locator]
   [xtdb.demo.web.var-based-locator :refer [resource-tree]]
   xtdb.demo.db
   xtdb.demo.resources
   xtdb.demo.static-resources))

(def http-server
  (run-server {:port 3000 :join? false
               :locator (fn [req]
                          (locator/find-resource
                           (resource-tree)
                           (:ring.request/path req)))}))
