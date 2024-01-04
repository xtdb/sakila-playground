(ns xtdb.demo.yry.resources
  {:web-context "/yry/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.demo.web.var-based-locator :refer [var->path]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node next-id]]
   [ring.util.codec :refer [form-decode]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]
   [xtdb.demo.web.request :refer [read-request-body]]
   [selmer.parser :as selmer])
  (:import [java.util Locale]
           [java.time.format TextStyle]
           [java.time Month Year]))



(def rentals-per-year-month-query
  "WITH rentals_ym AS (
     SELECT EXTRACT (YEAR FROM rental.xt$valid_from) as year,
            EXTRACT (MONTH FROM rental.xt$valid_from) as month
     FROM rental)
   SELECT A.year, A.month, count(*) as rented
   FROM rentals_ym as A
   GROUP BY A.year, A.month
   ORDER BY A.year DESC, A.month DESC")

(def rentals-per-category-query 
  "WITH rental_categories AS 
               (SELECT film_category.category_id as category_id,
                       count(*) as films_rented 
                FROM rental
                LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
                LEFT JOIN film_category ON inventory.film_id = film_category.film_id
                GROUP BY film_category.category_id)
          SELECT category.name AS category_name,
                 rental_categories.category_id, 
                 rental_categories.films_rented 
          FROM rental_categories 
          LEFT JOIN category ON rental_categories.category_id = category.xt$id
          ORDER BY category.name ASC")

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


(def default-query-params {:default-all-valid-time? true})

(defn rentals-per-year-month-data
  [{:keys [xt-node]}]
  (xt/q xt-node rentals-per-year-month-query default-query-params))

(defn rentals-per-category-data
  [{:keys [xt-node]}]
  (xt/q xt-node rentals-per-category-query default-query-params))

(defn month-num->month-name
  [num]
  (.getDisplayName (Month/of num) TextStyle/FULL Locale/UK))

(defn top-users-data
  [{:keys [xt-node]} & {:keys [limit] :or {limit 10}}]
  (xt/q xt-node
        (format top-users-raw-data limit)
        default-query-params))

(defn ^{:web-path "rentals"} rentals-per-year-month [x]
  (html-templated-resource
   {:template "templates/rental_analytics.html"
    :template-model
    {"rentals_ym"
     (fn [request] 
       (mapv #(update % :month month-num->month-name) (rentals-per-year-month-data xt-node)))
     "rentals_category"
     (fn [request] (rentals-per-category-data xt-node))
     "rentals_top_users"
     (fn [request] (top-users-data xt-node))}}))