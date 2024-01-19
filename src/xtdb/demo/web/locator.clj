(ns xtdb.demo.web.locator
  (:require
   [clojure.string :as str]
   [xtdb.demo.web.uri-template :refer [uri-template-matches]]
   [juxt.reap.decoders.rfc6570 :as uri-template]
   ))

(def compile-uri-template
  (memoize
   (fn [uri-template]
     (uri-template/compile-uri-template uri-template))))

(defn find-resource [resource-tree request-path]
  (first
   (for [{:keys [web-context resources]} resource-tree
         :when (str/starts-with? request-path web-context)
         {:keys [web-path uri-template resource-fn]} resources
         :let [resource
               (cond
                 web-path (when (= (str web-context web-path) request-path)
                            (resource-fn {}))
                 uri-template
                 (when-let [{:keys [vars]}
                            (uri-template/match-uri
                             (compile-uri-template (str web-context uri-template))
                             request-path)]
                   (resource-fn {:path-params vars})))]
         :when resource]
     resource)))
