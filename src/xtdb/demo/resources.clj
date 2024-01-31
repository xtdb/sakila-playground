(ns xtdb.demo.resources
  {:web-context "/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource templated-responder]]
   [xtdb.demo.web.locator :as locator]
   [xtdb.demo.web.var-based-locator :refer [var->path]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node next-id q]]
   [ring.util.codec :refer [form-decode]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]
   [xtdb.demo.web.request :refer [read-request-body]]
   [selmer.parser :as selmer]
   [clojure.java.io :as io])
  (:import (java.time LocalDate ZoneId)))

(defn hello [_]
  (let [state (atom {:greeting "Hello"})]
    (map->Resource
     {:representations
      [^{"content-type" "text/html;charset=utf-8"}
       (fn [request]
         {:ring.response/body (format "<h1>%s World!</h1>\r\n" (:greeting @state))})]})))

(defn ^{:web-path "index.html"} index [_]
  (html-templated-resource
   {:template "templates/index.html"
    :template-model {}}))

(defn ^{:web-path ""} index-redirect [_]
  (map->Resource
   {:methods
    {"GET" {:handler
            (fn [resource request]
              {:ring.response/status 302
               :ring.response/headers {"location" (var->path #'index)}})}}}))

(def select-films
  "SELECT film.xt$id AS id, film.title, film.description FROM film ORDER BY film.title")

(defn films-no-template [_]
  (html-resource
   (fn [_]
     (->
      (q select-films)
      (html/html-table {:rowspecs [:title :description]})
      (h/html)
      (str "\r\n")))))

(defn films [_]
  (html-templated-resource
   {:template "templates/films.html"
    :template-model
    {"films"
     (fn [request]
       (let [rows (q select-films)
             query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             q (get query-params "q")]
         (if q
           (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" q "\\E" ".*")) (str (:title row) (:description row)))) rows)
           rows
           )))}}))

(defn ^{:web-path "films/new"} films-new [_]
  (let [template "templates/new-film.html"
        template-model {}]
    (map->Resource
     {:methods
      {"POST"
       {:handler
        (fn [resource req]
          ;; TODO: Insert into database
          ;; Redirect
          {:ring.response/status 302
           :ring.response/headers {"location" (var->path #'films)}})}}
      :representations
      [^{"content-type" "text/html;charset=utf-8"}
       (fn [req]
         (let [query-params (when-let [query (:ring.request/query req)]
                              (form-decode query))]
           (selmer/render-file
            template
            (cond-> template-model
              query-params (assoc "query_params" query-params)))))]})))

#_(let [id 1]
  (xt/q (:xt-node xt-node) (format "select film.xt$id as id.*, language.name as language from film, language where film.xt$id = %s and language.xt$id = film.language_id" id)))

#_(let [id 1]
  (xt/q (:xt-node xt-node) (format "select film.xt$id as id.*, language.name as language from film, language where film.xt$id = %s and language.xt$id = film.language_id" id)))


(def ^:dynamic *language* :xtql)

;; See https://www.jooq.org/img/sakila.png

(defn ^{:uri-template "films/{id}"} film [{:keys [path-params]}]
  (let [id (get path-params "id")]
    (html-templated-resource
     {:template "templates/film.html"
      :template-model
      {"film"
       (let [film (first
                   (case *language*
                     :sql (q "SELECT film.*, language.name AS language FROM film LEFT JOIN language ON language.xt$id = film.language_id WHERE film.xt$id = ?"
                             {:args [(Long/parseLong id)]})

                     :xtql (q '(unify (from :film [{:xt/id $film-id} title description language_id release_year rating])
                                      (from :language [{:xt/id language_id} {:name language}]))
                              {:args {:film-id (Long/parseLong id)}
                               })))]
         (assoc film :id id))}})))

(defn customers-table [_]
  (let [customers (fn [request]
                    (let [rows (q "SELECT customer.xt$id AS id, customer.first_name, customer.last_name FROM customer ORDER BY customer.last_name")
                          query-params (when-let [query (:ring.request/query request)]
                                         (form-decode query))
                          q (get query-params "q")]
                      (if q
                        (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" q "\\E" ".*")) (str (:id row) (:first_name row) (:last_name row)))) rows)
                        rows
                        )))]
    (map->Resource
     {:representations
      [(templated-responder
        "templates/customers-table.html" "text/html;charset=utf-8"
        {"customers" customers})

       ^{"content-type" "application/edn"}
       (fn [req] {:ring.response/body (pr-str (customers req))})]})))

(defn customers [_]
  (html-templated-resource
   {:template "templates/customers-page.html"
    :template-model
    {}}))

(comment
  (xt/q (:xt-node xt-node) "SELECT * FROM customer"))

(defn ^{:web-path "customers/new"} customers-new [_]
  (let [template "templates/new-customer.html"
        template-model {}]
    (map->Resource
     {:methods
      {"POST"
       {:accept ["application/x-www-form-urlencoded"]
        :handler
        (fn [resource request]
          ;; How to get form data from the request?
          ;; See receive_representation
          (let [body (read-request-body resource request)]
            (xt/submit-tx
             (:xt-node xt-node)
             [[:put-docs :customer
               (into (update-keys body keyword)
                     {:xt/id (next-id :customer)
                      :active true})]]))
          ;; Redirect
          {:ring.response/status 302
           :ring.response/headers {"location" (var->path #'customers)}})}}
      :representations
      [^{"content-type" "text/html;charset=utf-8"}
       (fn [req]
         (let [query-params (when-let [query (:ring.request/query req)]
                              (form-decode query))]
           (selmer/render-file
            template
            (cond-> template-model
              query-params (assoc "query_params" query-params)))))]})))

(defn ^{:uri-template "customers/{id}/historic-rentals{.suffix}"}
  customer-historic-rentals
  [{:keys [path-params]}]
  (let [suffix (get path-params "suffix")
        sql (slurp "resources/sql/customer-historic-rentals.sql")
        customer-id (Long/parseLong (get path-params "id"))]
    (map->Resource
     {:representations
      (case suffix
        "sql" [^{"content-type" "text/plain"}
               (fn [req]
                 {:ring.response/body sql})]
        "html" [^{"content-type" "text/html"}
                (templated-responder
                 "templates/customer-historic-rentals.html"
                 "text/html;charset=utf-8"
                 {"rows"
                  (q (slurp "resources/sql/customer-historic-rentals.sql")
                     {:args [customer-id]})})]
        [])})))

(defn ^{:uri-template "customers/{id}/"} customer [{:keys [path-params]}]
  (let [customer-id (Long/parseLong (get path-params "id"))]

    (html-templated-resource
     {:template "templates/customer.html"
      :template-model
      {"customer"
       (first
        (xt/q
         (:xt-node xt-node)
         '(unify (from :customer [{:xt/id $customer_id} {:xt/id id} first_name last_name email address_id])
                 (from :address [{:xt/id address_id} phone]))
         {:args {:customer_id customer-id}
          }))

       "current_rentals"
       (q '(unify (from :rental {:bind [{:xt/id rental_id} {:xt/id id} {:customer-id $customer_id} inventory_id {:xt/valid-from rental_date}]})
                  (from :customer [{:xt/id $customer_id}])
                  (from :inventory [{:xt/id inventory_id} film_id])
                  (from :film [{:xt/id film_id} title]))
          {:args {:customer_id customer-id}})

       }})))

(defn ^{:uri-template "customers/{id}/detail"} customer-detail [{:keys [path-params]}]
  (html-resource
   (fn [_]
     (let [customer (first (xt/q (:xt-node xt-node) (format "SELECT customer.xt$id AS id, customer.first_name, customer.last_name, customer.email, address.phone FROM customer, address WHERE customer.xt$id = %s AND customer.address_id = address.xt$id" (get path-params "id"))))]
       {:ring.response/body
        (str (h/html [:dl
                      [:dt "First name"]
                      [:dd (:first_name customer)]
                      [:dt "Last name"]
                      [:dd (:last_name customer)]
                      [:dt "Email"]
                      [:dd (:email customer)]
                      [:dt "Phone"]
                      [:dd (:phone customer)]]))}))))

(comment
  (take 10 (xt/q (:xt-node xt-node) "select customer.xt$id as id, customer.* from customer")))

(comment
  (take 10 (xt/q (:xt-node xt-node) "select address.xt$id as id, address.* from address")))

(defn languages [_]
  (html-templated-resource
   {:template "templates/languages.html"
    :template-model
    {"languages" (fn [_] (xt/q
                          (:xt-node xt-node)
                          "SELECT language.xt$id, language.name FROM language ORDER BY xt$id"))}}))

(defn rentals [_]
  (html-templated-resource
   {:template "templates/rentals.html"
    :template-model
    {"rentals" (q '(unify (from :rental {:bind [{:xt/id rental_id} {:xt/id id} {:customer-id $customer_id} inventory_id {:xt/valid-from rental_date}]})
                          (from :customer [{:xt/id $customer_id} {:first_name first_name} {:last_name last_name}])
                          (from :inventory [{:xt/id inventory_id} film_id])
                          (from :film [{:xt/id film_id} {:title film}]))
                  {:args {:customer_id 560}
                   })}}))

(defn ^{:uri-template "rentals/{id}"} rental [{:keys [path-params]}]
  (let [rental-id (Long/parseLong (get path-params "id"))]
    (map->Resource
     {:methods
      {"DELETE"
       {:accept ["application/x-www-form-urlencoded"]
        :handler
        (fn [resource req]
          (let [{:strs [return-date]} (read-request-body resource req)
                return-date (when (not-empty return-date)
                              (try
                                (LocalDate/parse return-date)
                                (catch Exception _
                                  (throw (ex-info "Invalid return date" {:ring.response/status 400})))))
                return-instant (some-> return-date (.atStartOfDay (ZoneId/of "Europe/London")))]

            (println "Deleting" rental-id)

            (xt/submit-tx
              (:xt-node xt-node)
              [[:delete-docs {:from :rental, :valid-from return-instant}
                rental-id]])

            ;; returning 204 causes HTMX to not swap, even if a hx-swap=delete is set.
            {:ring.response/status 200}))}}})))

(defn analytics [_]
  (let [rows (xt/q (:xt-node xt-node)
                   (format
                    "SELECT A.year, A.month, count(*) as rented
           FROM (%s) as A
           GROUP BY A.year, A.month
           ORDER BY A.year DESC, A.month DESC
           " "SELECT EXTRACT (YEAR FROM rental.xt$valid_from) as year,
          EXTRACT (MONTH FROM rental.xt$valid_from) as month
   FROM rental FOR ALL VALID_TIME")
                   )]
    (map->Resource
     {:representations
      [^{:content-type "text/html;charset=utf-8"}(fn [_] (str (h/html
                       [:div
                        (for [{:keys [year month rented]} rows]
                          [:table
                           [:thead
                            [:th "Year/Month"]
                            [:th "Videos rented"]]
                           [:tbody
                            [:td (java.time.YearMonth/of year month)]
                            [:td rented]
                            ]]
                          )]
                       )))]}))
  )
