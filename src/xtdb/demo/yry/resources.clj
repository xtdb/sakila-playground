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
  [query & args]
  (try
    {:result (xt/q (:xt-node xt-node) query (cond-> default-query-params
                                              args (assoc :args args)))}
    (catch Exception e
      {:error (ex-message e)})))

(defn ->sql-date [x]
  (when (and (string? x) (seq x)) (java.sql.Date/valueOf x)))

(selmer/add-filter! :query sql-query)
(selmer/add-filter! :to-sql-date ->sql-date)
(selmer/add-filter! :resource-load (comp slurp resource))
(selmer/add-filter! :url-load slurp)

(defn ^{:uri-template "rental-per-month/{view}"}
  rental-per-month [{:keys [path-params]}]
  (html-templated-resource
   {:template "templates/rental-analytics/rentals-per-month.html"
    :template-model {"selected" (get path-params "view")}}))

(defn ^{:uri-template "rental-per-category/{view}"}
  rental-per-category [{:keys [path-params]}]
  (html-templated-resource
   {:template       "templates/rental-analytics/rentals-per-category.html"
    :template-model {"selected" (get path-params "view")}}))

(defn ^{:uri-template "top-renting-customers/{view}"}
  top-renting-customers [{:keys [path-params]}]
  (html-templated-resource
   {:template "templates/rental-analytics/top-renting-customers.html"
    :template-model {"selected" (get path-params "view")}}))

(defn ^{:uri-template "top-rented-films/{view}"}
  top-rented-films [{:keys [path-params]}]
  (html-templated-resource
   {:template "templates/rental-analytics/top-rented-films.html"
    :template-model {"selected" (get path-params "view")}}))

(defn ^{:web-path "rentals"} 
  rentals-per-year-month [_]
  (html-templated-resource
   {:template "templates/rental_analytics.html"}))