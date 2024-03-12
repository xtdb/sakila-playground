;; Copyright © 2024, JUXT LTD.

(ns xtdb.demo.xtsql
  {:web-context "/jdt/"}
  (:require
   [xtdb.demo.web.resource :refer [file-resource]]
   [clojure.java.io :as io]))

(defn ^{:uri-template "xtsql.py"
        :uri-variables {:view :string}}
  xtsql [_]
  (file-resource (io/file "src/xtsql.py") "text/plain"))
