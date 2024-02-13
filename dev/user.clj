(ns user
  (:require
    [nextjournal.clerk :as clerk]
    [xtdb.demo.web.server :refer [run-server]]
    [xtdb.demo.web.locator :as locator]
    [xtdb.demo.web.var-based-locator :refer [resource-tree]]
    xtdb.demo.db
    xtdb.demo.resources
    xtdb.demo.sql
    xtdb.demo.jdt-resources
    xtdb.demo.yry.resources
    xtdb.demo.static-resources))

(declare http-server)

(when (not= (type http-server) clojure.lang.Var$Unbound)
  (.stop http-server))

(def http-server
  (run-server {:port 3000
               :join? false
               :locator (fn [req]
                          (locator/find-resource
                           (resource-tree)
                           (:ring.request/path req)))}))

(clerk/serve! {:watch-paths ["dev/nb"]})
