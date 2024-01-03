(ns xtdb.demo.web.locator
  (:require
   [clojure.string :as str]))

(def to-regex
  (memoize
   (fn [uri-template]
     (re-pattern
      (str/replace
       uri-template
       #"\{([^\}]+)\}"                  ; e.g. {id}
       (fn replacer [[_ group]]
         (format "(?<%s>[^/#\\?]+)" group)))))))

(defn uri-template-matches [resource-path request-path]
  (let [regex (to-regex resource-path)
        groups (re-matches regex request-path)]
    (when (first groups)
      (zipmap
       (map second (re-seq #"\{(\p{Alpha}+)\}" resource-path))
       (next groups)))))

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
                 (when-let [params (uri-template-matches
                                    (str web-context uri-template)
                                    request-path)]
                   (resource-fn {:path-params params})))]
         :when resource]
     resource)))
