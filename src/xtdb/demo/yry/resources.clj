(ns xtdb.demo.yry.resources
  {:web-context "/yry/"}
  (:require
   [xtdb.demo.web.resource :refer [html-templated-resource]]
   [xtdb.demo.db :refer [xt-node]]
   [xtdb.api :as xt]
   [selmer.parser :as selmer]
   [clojure.java.io :refer [resource]]))

(def default-query-params {:default-all-valid-time? true})

(defn sql-query
  [query]
  (let [result (xt/q (:xt-node xt-node) query default-query-params)]
    result))

(selmer/add-filter! :query sql-query)
(selmer/add-filter! :resource-load (comp slurp resource))
(selmer/add-filter! :url-load slurp)

(defn ^{:web-path "rental-per-month"}
  rental-per-month [_]
  (html-templated-resource
   {:template "templates/rental-analytics/rentals-per-month.html"}))

(defn ^{:web-path "rental-per-category"}
  rental-per-category [_]
  (html-templated-resource
   {:template "templates/rental-analytics/rentals-per-category.html"}))

(defn ^{:web-path "top-renting-customers"}
  top-renting-customers [_]
  (html-templated-resource
   {:template "templates/rental-analytics/top-renting-customers.html"}))

(defn ^{:web-path "top-rented-films"}
  top-rented-films [_]
  (html-templated-resource
   {:template "templates/rental-analytics/top-rented-films.html"}))

(defn ^{:web-path "rentals"} rentals-per-year-month [x]
  (html-templated-resource
   {:template "templates/rental_analytics.html"}))