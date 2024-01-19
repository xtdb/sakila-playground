(ns xtdb.demo.web.uri-template
  (:require
   [clojure.string :as str]
   [juxt.reap.combinators :as p]
   ))

(def to-regex
  (memoize
   (fn [uri-template]
     (re-pattern
      (str/replace
       uri-template
       #"\{([^\}]+)\}"                  ; e.g. {id}
       (fn replacer [[_ group]]
         (format "(?<%s>[\\p{Alnum}]+)" group)))))))

#_(defn expression [_]
  (p/sequence)
  )

#_(defn to-regex [uri-template]
  (re-pattern
   (str/replace
    uri-template
    #"\{([^\}]+)\}"
    (fn replacer [[_ expression]]
      (format "(?<%s>[[]?[\\p{Alpha}]+)" expression)))))

(defn uri-template-matches [resource-path request-path]
  (let [regex (to-regex resource-path)
        groups (re-matches regex request-path)]
    (when (first groups)
      (zipmap
       (map second (re-seq #"\{(\p{Alpha}*)\}" resource-path))
       (next groups)))))


;;(uri-template-matches "/foo/{x}{suffix}" "/foo/bar.sql")
;;(to-regex "/foo/{x}{suffix}")
