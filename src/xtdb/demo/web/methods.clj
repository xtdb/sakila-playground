(ns xtdb.demo.web.methods
  (:require
   [clojure.string :as str]))

(defn allowed-methods [resource request]
  (let [methods (set (keys (:methods resource {"GET" {}})))]
    (keep
     (fn [x] (case x
               "GET" (when (contains? methods "GET") x)
               "HEAD" (when (contains? methods "GET") x)
               "POST" (when (contains? methods "POST") x)
               "PUT" (when (contains? methods "PUT") x)
               "DELETE" (when (contains? methods "DELETE") x)
               "OPTIONS" (when true x)))
     ["GET" "HEAD" "POST" "PUT" "DELETE" "OPTIONS"])))

(defn select-representation [representations request]
  (when request
    ;; TODO: Use juxt/pick
    (first representations)))

(defn GET [resource request]
  (let [representation
        (-> resource :representations (select-representation request))]
    (if representation
      {:ring.response/status 200
       :ring.response/headers (-> representation meta :headers)
       :ring.response/body (representation request)}
      {:ring.response/status 404})))

(defn HEAD [resource request]
  (let [representation
        (-> resource :representations (select-representation request))]
    {:ring.response/status 200
     :ring.response/headers (-> representation meta :headers)}))

(defn PUT [resource request]
  (throw (ex-info "TODO" {})))

(defn POST [resource request]
  (if-let [f (get (:methods resource) "POST")]
    (let [{:keys [headers body]} (f request)]
      {:ring.response/status 201
       :ring.response/headers headers
       :ring.response/body body}
      )
    {:ring.response/status 405}))

(defn DELETE [resource request]
  (throw (ex-info "TODO" {})))

(defn OPTIONS [resource request]
  (let [methods (allowed-methods resource request)]
    {:ring.response/status 200
     :ring.response/headers {"allow" (str/join ", " methods)}}))
