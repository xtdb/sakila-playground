;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.static-resources
  {:web-context "/static/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource file-resource]]
   [clojure.java.io :as io]))

(def ^{:web-path "css/missing.css-1.1.1.js"} missing-css
  (file-resource (io/file "missing.css-1.1.1.js")))
