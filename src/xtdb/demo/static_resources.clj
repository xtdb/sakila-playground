;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.static-resources
  {:web-context "/static/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource file-resource]]
   [clojure.java.io :as io]))

(defn ^{:web-path "missing.css.css"} missing-css-css [_]
  (file-resource
   (io/file "external/missing.css-1.1.1.css")
   "text/css"))

(defn ^{:web-path "screen.css"} screen-css [_]
  (file-resource
   (io/file "resources/screen.css")
   "text/css"))

(defn ^{:web-path "htmx.org.js"} htmx-js [_]
  (file-resource
   (io/file "external/htmx.org-1.9.10.js")
   "text/javascript"))

(defn ^{:web-path "hyperscript.js"} hyperscript-js [_]
  (file-resource
   (io/file "external/hyperscript.org-0.9.12.js")
   "text/javascript"))

(defn ^{:web-path "juxt.svg"} juxt-logo [_]
  (file-resource
   (io/file "resources/images/juxt.svg")
   "image/svg+xml"))

(defn ^{:web-path "bar-chart.vg.json"} bar-chart [_]
  (file-resource
   (io/file "external/bar-chart.vg.json")
   "application/json"))

(defn ^{:web-path "movies.json"} movies [_]
  (file-resource
   (io/file "external/movies.json")
   "application/json"))
