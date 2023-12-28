;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.static-resources
  {:web-context "/static/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource file-resource]]
   [clojure.java.io :as io]))

(defn ^{:web-path "missing.css.css"} missing-css-css [_]
  (file-resource (io/file "external/missing.css-1.1.1.css")))

(defn ^{:web-path "site.css"} site-css [_]
  (file-resource (io/file "external/site.css")))

(defn ^{:web-path "htmx.org.js"} htmx-js [_]
  (file-resource (io/file "external/htmx.org-1.9.10.js")))

(defn ^{:web-path "hyperscript.js"} hyperscript-js [_]
  (file-resource (io/file "external/hyperscript.org-0.9.12.js")))
