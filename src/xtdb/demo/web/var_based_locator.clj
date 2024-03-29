(ns xtdb.demo.web.var-based-locator)

(defn resource-tree []
  (for [ns (all-ns)
        :when (:web-context (meta ns))]
    (assoc
     (meta ns)
     :resources
     (for [[nm v] (ns-publics ns)
           :let [m (meta v)
                 path (:web-path m)
                 uri-template (:uri-template m)
                 uri-variables (:uri-variables m)]]
       (cond
         path {:web-path path :resource-fn v}
         uri-template {:uri-template uri-template :uri-variables uri-variables :resource-fn v}
         :else {:web-path (name nm) :resource-fn v})))))

(defn var->path [v]
  (let [{:keys [ns web-path name]} (meta v)
        {:keys [web-context]} (meta ns)]
    (str web-context (or web-path name))))
