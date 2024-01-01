(ns xtdb.demo.web.locator
  (:require
   [clojure.string :as str]))

#_(defn all-web-context-namespaces []
    (->>
     (all-ns)
     (filter #(-> % meta :web-context))))

#_(defn filter-matching-namespaces-xf [path]
  (filter
   (fn [ns]
     (when-let [web-context (-> ns meta :web-context)]
       (when (.startsWith path web-context)
         ns)))))

#_(defn mapcat-matching-vars-xf [path]
  (mapcat
   (fn [ns]
     (let [web-context (-> ns meta :web-context)]
       (for [[nm v] (ns-publics ns)]
         (if (= (str web-context nm) path)
           v
           (let [{:keys [web-path]} (meta v)]
             (when (= (str web-context web-path) path)
               v))))))))

#_(defn locate-resource [request]
  (let [path (:ring.request/path request)
        resources (->>
                   (all-web-context-namespaces)
                   (sequence
                    (comp
                     (filter-matching-namespaces-xf path)
                     (mapcat-matching-vars-xf path)
                     (remove nil?))))
        resource-fn (first resources)]
    (when resource-fn (resource-fn {:request request}))))

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
