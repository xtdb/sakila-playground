(ns xtdb.demo.jdt-resources
  {:web-context "/jdt/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource templated-responder]]
   [xtdb.demo.web.locator :as locator]
   [xtdb.demo.web.var-based-locator :refer [var->path]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node next-id q sql-op]]
   [ring.util.codec :refer [form-decode]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]
   [xtdb.demo.web.request :refer [read-request-body]]
   [selmer.parser :as selmer]))

;; (sql-op "INSERT INTO film(xt$id, title, description, language_id) VALUES (9999, 'foo', 'bar', 1)")

(def select-available-films
  "SELECT film.xt$id AS id, film.title, film.description, inventory.store_id
FROM film
JOIN inventory ON (inventory.film_id = film.xt$id AND inventory.store_id = ?)
LEFT JOIN rental ON (rental.inventory_id = inventory.xt$id AND rental.return_date < CURRENT_TIMESTAMP)
ORDER BY film.title")

;; TODO datepicker

(defn available-films [_]
  (html-templated-resource
   {:template "templates/available-films.html"
    :template-model
    {"available-films"
     (fn [request]
       (let [query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             store-id (-> (get query-params "store")
                          Integer/parseInt)
             rows (q select-available-films {:args [(or store-id 1)]})
             filter-str (get query-params "q")]
         (if filter-str
           (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" q "\\E" ".*")) (str (:title row) (:description row)))) rows)
           rows
           )))
     "store-id"
     (fn [request]
       (let [query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             store-id (-> (get query-params "store")
                          Integer/parseInt)]
         store-id))}}))
