(ns xtdb.demo.web.resource
  (:require
   [xtdb.demo.web.protocols :refer [UniformInterface allowed-methods GET HEAD POST PUT DELETE OPTIONS]]
   [xtdb.demo.web.methods :as methods]
   [ring.util.codec :refer [form-decode]]
   [selmer.parser :as selmer]
   [juxt.reap.encoders :refer [format-http-date]]
   [clojure.java.io :as io]))

(selmer/cache-off!)

(defrecord Resource [m]
  UniformInterface
  (allowed-methods [resource request]
    (methods/allowed-methods resource request))

  (GET [resource request]
    (methods/GET resource request))

  (HEAD [resource request]
    (methods/HEAD resource request))

  (PUT [resource request]
    (methods/PUT resource request))

  (POST [resource request]
    (methods/POST resource request))

  (DELETE [resource request]
    (methods/DELETE resource request))

  (OPTIONS [resource request]
    (methods/OPTIONS resource request)))

(def not-found-resource
  (map->Resource
   {:representations []
    :responses {404 (fn [_]
                      {:ring.response/headers {"content-type" "text/plain;charset=utf-8"}
                       :ring.response/body "Not found\r\n"})}}))

(extend-protocol UniformInterface
  clojure.lang.Var
  (GET [v request] (GET @v request))
  (HEAD [v request] (HEAD @v request))
  (PUT [v request] (PUT @v request))
  (POST [v request] (POST @v request))
  (DELETE [v request] (DELETE @v request))
  (OPTIONS [v request] (OPTIONS @v request))

  nil
  (GET [_ request] (GET not-found-resource request))
  (HEAD [_ request] (HEAD not-found-resource request))
  (PUT [_ request] (PUT not-found-resource request))
  (POST [_ request] (POST not-found-resource request))
  (DELETE [_ request] (DELETE not-found-resource request))
  (OPTIONS [_ request] (OPTIONS not-found-resource request)))

(defn html-resource [f]
  (map->Resource
   {:representations
    [(with-meta f {"content-type" "text/html;charset=utf-8"})]}))

(defn templated-responder [template content-type template-model]
  ^{"content-type" content-type}
  (fn [req]
    {:body
     (let [template-model (update-vals template-model (fn [x] (if (fn? x) (x req) x)))
           query-params (when-let [query (:ring.request/query req)]
                          (form-decode query))]
       (selmer/render-file
        template
        (cond-> template-model
          query-params (assoc "query_params" query-params))))}))

(defn html-templated-resource [{:keys [template template-model]}]
  (map->Resource
   {:representations
    [(templated-responder template "text/html;charset=utf-8" template-model)]}))

(defn file-resource [file content-type]
  (map->Resource
   {:representations
    [^{"content-type" content-type
       "last-modified" (format-http-date (java.util.Date. (.lastModified file)))}
     (fn [req]
       {:body file})]}))
