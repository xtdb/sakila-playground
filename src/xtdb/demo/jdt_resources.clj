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
   [java.time Instant LocalDateTime ZonedDateTime ZoneId]
   [java.time.format DateTimeFormatter]
   [xtdb.api TransactionKey]))

;; (sql-op "INSERT INTO film(xt$id, title, description, language_id) VALUES (9999, 'foo', 'bar', 1)")

(def select-available-films
  "SELECT DISTINCT f.xt$id AS id, f.title, f.description, COUNT(i.xt$id) AS inventory_count
FROM film f
JOIN inventory i ON (i.film_id = f.xt$id AND i.store_id = ?)
LEFT JOIN rental r ON (r.inventory_id = i.xt$id)
WHERE r.return_date IS NULL
GROUP BY f.xt$id, f.title, f.description
ORDER BY f.title")

(defn iso-string->instant [iso-string]
  (let [localDateTime (LocalDateTime/parse iso-string (DateTimeFormatter/ISO_LOCAL_DATE_TIME))
        zoneId (ZoneId/of "UTC")
        zonedDateTime (ZonedDateTime/of localDateTime zoneId)]
    (.toInstant zonedDateTime)))

(defn instant->iso-string [instant]
  (let [zoneId (ZoneId/of "UTC")
        localDateTime (LocalDateTime/ofInstant instant zoneId)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss.SSS")]
    (.format localDateTime formatter)))

(instant->iso-string #time/instant "2020-01-01T00:00:00.000Z")

(defn get-timestamp [query-params name]
  (or (some-> (get query-params name) iso-string->instant)
      (Instant/now)))

(defn available-films [_]
  (html-templated-resource
   {:template "templates/available-films.html"
    :template-model
    {"available_films"
     (fn [request]
       (let [query-params (when-let [query (:query-string request)]
                            (form-decode query))
             store-id (let [s (get query-params "store")]
                        (if (empty? s)
                          1
                          (Integer/parseInt s)))
             st (get-timestamp query-params "as_of_timestamp")
             vt (get-timestamp query-params "vt_timestamp")
             rows (q select-available-films
                     {:args [store-id]
                      :basis {:at-tx (TransactionKey. -1 st)}})
             filter-str (get query-params "q")]
         (if filter-str
           (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" filter-str "\\E" ".*")) (str (:title row) (:description row)))) rows)
           rows
           )))
     "store_id"
     (fn [request]
       (let [query-params (when-let [query (:query-string request)]
                            (form-decode query))
             store-id (let [s (get query-params "store")]
                        (if (empty? s)
                          1
                          (Integer/parseInt s)))]
         store-id))
     "vt_timestamp"
     (fn [request]
       (let [query-params (when-let [query (:query-string request)]
                            (form-decode query))]
         (-> (get-timestamp query-params "vt_timestamp")
             instant->iso-string)))
     "as_of_timestamp"
     (fn [request]
       (let [query-params (when-let [query (:query-string request)]
                            (form-decode query))]
         (-> (get-timestamp query-params "as_of_timestamp")
             instant->iso-string)))}}))
