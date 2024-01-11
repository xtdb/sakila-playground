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
   [selmer.parser :as selmer])
  (:import
   [java.time LocalDate Instant]
   [java.time.format DateTimeFormatter]
   [xtdb.api TransactionKey]))

;; (sql-op "INSERT INTO film(xt$id, title, description, language_id) VALUES (9999, 'foo', 'bar', 1)")

(def select-available-films
  "SELECT DISTINCT film.xt$id AS id, film.title, film.description, COUNT(inventory.xt$id) AS inventory_count
FROM film
JOIN inventory ON (inventory.film_id = film.xt$id AND inventory.store_id = 1)
LEFT JOIN rental ON (rental.inventory_id = inventory.xt$id AND rental.return_date < CURRENT_DATE)
GROUP BY film.xt$id, film.title, film.description
ORDER BY film.title")

(comment
  (def select-available-films
    "SELECT film.xt$id AS id, film.title, film.description, film.xt$valid_from
FROM film
ORDER BY film.xt$id DESC")

  (q select-available-films))

(defn get-vt [query-params]
  (or (get query-params "vt_date")
      (.format (.plusDays (LocalDate/now) 1) (DateTimeFormatter/ofPattern "yyyy-MM-dd"))))

(defn available-films [_]
  (html-templated-resource
   {:template "templates/available-films.html"
    :template-model
    {"available_films"
     (fn [request]
       (let [query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             store-id (let [s (get query-params "store")]
                        (if (empty? s)
                          1
                          (Integer/parseInt s)))
             vt (get-vt query-params)
             _ (prn (Instant/parse (str vt "T00:00:00Z")))
             rows (q select-available-films
                     {:args [store-id]
                      :basis {:current-time (Instant/now)
                              ;; (Instant/parse (str vt "T00:00:00Z"))
                              ;; TODO can't naively use current-time as not all tables are loaded with vt
                              #_ #_:at-tx (TransactionKey. -1 (Instant/parse "2024-01-11T22:00:00Z"))}})
             filter-str (get query-params "q")]
         (if filter-str
           (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" q "\\E" ".*")) (str (:title row) (:description row)))) rows)
           rows
           )))
     "store_id"
     (fn [request]
       (let [query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             store-id (let [s (get query-params "store")]
                        (if (empty? s)
                          1
                          (Integer/parseInt s)))]
         store-id))
     "vt_date"
     (fn [request]
       (let [query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))]
         (get-vt query-params)))
     "st_date"
     (fn [_]
       (.format (LocalDate/now) (DateTimeFormatter/ofPattern "yyyy-MM-dd")))}}))
