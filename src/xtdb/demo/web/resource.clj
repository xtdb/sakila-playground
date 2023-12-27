(ns xtdb.demo.web.resource
  (:require
   [xtdb.demo.web.protocols :refer [UniformInterface allowed-methods GET HEAD POST PUT DELETE OPTIONS]]
   [xtdb.demo.web.methods :as methods]))

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
   {:representations []}))

(extend-protocol UniformInterface
  clojure.lang.Var
  (GET [v request] (GET @v request))
  (HEAD [v request] (HEAD @v request))
  (POST [v request] (POST @v request))
  (OPTIONS [v request] (OPTIONS @v request))

  nil
  (GET [v request] (GET not-found-resource request))
  (HEAD [v request] (HEAD not-found-resource request))
  (POST [v request] (POST not-found-resource request))
  (OPTIONS [v request] (OPTIONS not-found-resource request)))

(defn html-resource [f]
  (map->Resource
   {:representations
    [(with-meta f {:headers {"content-type" "text/html;charset=utf-8"}})]}))
