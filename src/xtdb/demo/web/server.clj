(ns xtdb.demo.web.server
  (:require
   [clojure.set :as set]
   [xtdb.demo.web.locator :as locator]
   xtdb.demo.web.resource))

(defn wrap-ring-1-adapter
  "Given the presence of keywords from different origins, it helps that we
  distinguish Ring keywords from application keywords. Ring 2.0 provides a good
  set of namespaced keywords. This Ring middleware adapts a Ring 1.0 adapter to
  this set of keywords. See
  https://github.com/ring-clojure/ring/blob/2.0/SPEC-2.md for full details."
  [h]
  (fn [req]
    (let [mp {:body :ring.request/body
              :headers :ring.request/headers
              :request-method :ring.request/method
              :uri :ring.request/path
              :query-string :ring.request/query
              :protocol :ring.request/protocol
              :remote-addr :ring.request/remote-addr
              :scheme :ring.request/scheme
              :server-name :ring.request/server-name
              :server-port :ring.request/server-port
              :ssl-client-cert :ring.request/ssl-client-cert}]

      (-> (reduce-kv
           (fn [acc k v]
             (let [k2 (get mp k)]
               (cond-> acc k2 (assoc k2 v))))
           {} req)
          (h)
          (select-keys
           [:ring.response/status
            :ring.response/headers
            :ring.response/body])
          (set/rename-keys {:ring.response/status :status
                            :ring.response/headers :headers
                            :ring.response/body :body})))))

(def handler
  (->
   (fn [req]
     (locator/handle-request req))
   (wrap-ring-1-adapter)))
