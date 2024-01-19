(ns xtdb.demo.yry.resources
  {:web-context "/yry/"}
  (:require
   [xtdb.demo.web.resource :refer [html-templated-resource]]
   [xtdb.demo.db :refer [xt-node]]
   [xtdb.api :as xt]
   [selmer.parser :as selmer]
   [clojure.java.io :refer [resource]]))

(def top-users-raw-data
  "WITH top_user AS 
      (SELECT rental.customer_id, 
              count(*) AS films_rented
       FROM rental
       GROUP BY rental.customer_id
       ORDER BY films_rented DESC
       LIMIT %s)
   SELECT top_user.customer_id, 
          top_user.films_rented,
          customer.last_name,
          customer.first_name,
          customer.email            
   FROM top_user
   LEFT JOIN customer ON top_user.customer_id = customer.xt$id")

(def top-perfomer-film-raw-data
  "WITH film_rented AS 
      (SELECT inventory.film_id, 
             count(*) as count_rented
      FROM rental
      LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
      GROUP BY inventory.film_id
      ORDER BY count_rented DESC
      LIMIT %s)
   SELECT film_rented.film_id,
           film.title, 
           film_rented.count_rented 
   FROM film_rented
   LEFT JOIN film ON film_rented.film_id = film.xt$id")

(def default-query-params {:default-all-valid-time? true})

(defn sql-query
  [query]
  (let [result (xt/q (:xt-node xt-node) query default-query-params)]
    result))


(selmer/add-filter! :query sql-query)
(selmer/add-filter! :resource-load (comp slurp resource))
(selmer/add-filter! :url-load slurp)

(defn top-users-data
  [{:keys [xt-node]} & {:keys [limit] :or {limit 10}}]
  (xt/q xt-node
        (format top-users-raw-data limit)
        default-query-params))

(defn top-performed-films-data
  [{:keys [xt-node]} & {:keys [limit] :or {limit 10}}]
  (xt/q xt-node
        (format top-perfomer-film-raw-data limit)
        default-query-params))

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
   {:template "templates/rental-analytics/top-renting-customers.html"
    :template-model
    {"rentals_top_users"
     (fn [request] (top-users-data xt-node))}}))

(defn ^{:web-path "top-rented-films"}
  top-rented-films [_]
  (html-templated-resource
   {:template "templates/rental-analytics/top-rented-films.html"
    :template-model
    {"rentals_top_films"
     (fn [request] (top-performed-films-data xt-node))}}))

(defn ^{:web-path "rentals"} rentals-per-year-month [x]
  (html-templated-resource
   {:template "templates/rental_analytics.html"}))


(comment 
  
  (selmer/add-filter! :slurp (comp slurp clojure.java.io/resource))
  
  (selmer/add-filter! :many-args (fn [& x] (tap> {:so-many-args x}) x))
  
  (selmer/render "{% with path=\"sql/rentals-by-category.sql\" %}  {{path|slurp|query}} {% endwith %}" {})
  
  (slurp (clojure.java.io/resource "sql/rentals-by-category.sql"))
  
  
  (tap> :potap)
  (selmer/render "{{ 5|many-args:1:2:3}}" {})
  
  
  (clojure.java.io/resource "https://raw.githubusercontent.com/xtdb/sakila-playground/htmx/resources/sql/rentals-by-category.sql")
  
  (selmer/render "{{ x|date:\"mm\" }}" {:x (java.util.Date.)})
  
  )