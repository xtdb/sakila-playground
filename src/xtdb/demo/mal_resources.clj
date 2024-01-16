(ns xtdb.demo.mal-resources
  {:web-context "/mal/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.demo.web.var-based-locator :refer [var->path]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node next-id]]
   [ring.util.codec :refer [form-decode]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]
   [xtdb.demo.web.request :refer [read-request-body]]
   [selmer.parser :as selmer]
   [clojure.string :as str]
   [clojure.java.io :as io])
  )

(let [sql (slurp (io/file "resources/sql/rentals-by-category.sql"))
      resultset (xt/q (:xt-node xt-node) sql {:default-all-valid-time? true})]
  resultset
  )

(defn rentals-by-category [_]
  (let [sql (slurp (io/file "resources/sql/rentals-by-category.sql"))
        resultset (xt/q (:xt-node xt-node) sql {:default-all-valid-time? true})]
    (map->Resource
     {:representations
      [
       ^{"content-type" "text/plain"}
       (fn [req] {:ring.response/body (pr-str resultset)})

       ^{"content-type" "text/html"}
       (fn [req] {:ring.response/body
                  (selmer/render-file
                   "templates/mal/rentals-by-category.html"
                   {"resultset" resultset})})

       ^{"content-type" "application/sql"}
       (fn [req] {:ring.response/body sql})]

      })))

(defn rental-analytics [_]
  (html-templated-resource
   {:template "templates/mal/rental-analytics.html"
    :template-model
    {}}))