(ns xtdb.demo.web.locator
  (:require
   [clojure.string :as str]
   [juxt.reap.rfc6570 :as uri-template]
   ))

(def compile-uri-template
  (memoize
   (fn [uri-template]
     (uri-template/compile-uri-template uri-template))))

(defn find-resource [resource-tree request-path]
  (first
   (for [{:keys [web-context resources]} resource-tree
         :when (str/starts-with? request-path web-context)
         {:keys [web-path uri-template uri-variables resource-fn]} resources
         :let [resource
               (cond
                 web-path (when (= (str web-context web-path) request-path)
                            (resource-fn {}))
                 uri-template
                 (do
                   (when-not uri-variables
                     (throw (ex-info "Missing uri variables" {:resource-fn resource-fn})))

                   (when-let [vals
                              (uri-template/match-uri
                               (compile-uri-template (str web-context uri-template))
                               uri-variables
                               request-path)]
                     (when-not vals
                       (throw
                        (ex-info
                         "No vars matched"
                         {:uri-template (str web-context uri-template)
                          :uri-variables uri-variables
                          :request-path request-path})))

                     (resource-fn vals))))]
         :when resource]
     resource)))
