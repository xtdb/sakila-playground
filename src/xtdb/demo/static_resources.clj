;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.static-resources
  {:web-context "/static/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource file-resource]]
   [clojure.java.io :as io]))

(def ^{:web-path "missing.css.css"} missing-css
  (file-resource (io/file "external/missing.css-1.1.1.css")))

(def ^{:web-path "htmx.org.js"} htmx
  (file-resource (io/file "external/htmx.org-1.9.10.js")))

(def ^{:web-path "hyperscript.js"} hyperscript
  (file-resource (io/file "external/hyperscript.org-0.9.12.js")))
