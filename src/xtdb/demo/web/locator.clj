(ns xtdb.demo.web.locator
  (:require
   [xtdb.demo.web.protocols :refer [GET HEAD POST PUT DELETE OPTIONS]]))

(defn all-web-context-namespaces []
  (->>
   (all-ns)
   (filter #(-> % meta :web-context))))

(defn filter-matching-namespaces-xf [path]
  (filter
   (fn [ns]
     (when-let [web-context (-> ns meta :web-context)]
       (when (.startsWith path web-context)
         ns)))))

(defn mapcat-matching-vars-xf [path]
  (mapcat
   (fn [ns]
     (let [web-context (-> ns meta :web-context)]
       (for [[nm v] (ns-publics ns)]
         (if (= (str web-context nm) path)
           v
           (let [{:keys [web-path]} (meta v)]
             (when (= (str web-context web-path) path)
               v))))))))

(defn handle-request [request]
  (let [path (:ring.request/path request)
        resources (->>
                   (all-web-context-namespaces)
                   (sequence
                    (comp
                     (filter-matching-namespaces-xf path)
                     (mapcat-matching-vars-xf path)
                     (remove nil?))))
        resource-fn (first resources)
        resource (resource-fn {:request request})]
    (case (:ring.request/method request)
      :get (GET resource request)
      :head (HEAD resource request)
      :post (POST resource request)
      :put (PUT resource request)
      :delete (DELETE resource request)
      :options (OPTIONS resource request)
      {:ring.response/status 501})))
