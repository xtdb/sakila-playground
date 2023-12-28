;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.static-resources
  {:web-context "/static/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource file-resource]]
   [clojure.java.io :as io]))

(defn ^{:web-path "missing.css.css"} missing-css [_]
  (file-resource (io/file "external/missing.css-1.1.1.css")))

(defn ^{:web-path "htmx.org.js"} htmx [_]
  (file-resource (io/file "external/htmx.org-1.9.10.js")))

(defn ^{:web-path "hyperscript.js"} hyperscript [_]
  (file-resource (io/file "external/hyperscript.org-0.9.12.js")))
