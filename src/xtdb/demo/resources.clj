(ns xtdb.demo.resources
  {:web-context "/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.demo.web.locator :as locator]
   [xtdb.demo.web.var-based-locator :refer [var->path]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node next-id]]
   [ring.util.codec :refer [form-decode]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]
   [xtdb.demo.web.request :refer [read-request-body]]
   [selmer.parser :as selmer]))

(defn hello [_]
  (let [state (atom {:greeting "Hello"})]
    (map->Resource
     {:representations
      [^{:headers {"content-type" "text/html;charset=utf-8"}}
       (fn [] (format "<h1>%s World!</h1>\r\n" (:greeting @state)))
       ]})))

(def select-films
  "select film.xt$id as id, film.title, film.description from film order by film.title")

(defn films-no-template [_]
  (html-resource
   (fn [_]
     (->
      (xt/q (:xt-node xt-node) select-films)
      (html/html-table {:rowspecs [:title :description]})
      (h/html)
      (str "\r\n")))))

(defn films [_]
  (html-templated-resource
   {:template "templates/films.html"
    :template-model
    {"films"
     (fn [request]
       (let [rows (xt/q (:xt-node xt-node) select-films)
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
      [^{:headers {"content-type" "text/html;charset=utf-8"}}
       (fn [req]
         (let [query-params (when-let [query (:ring.request/query req)]
                              (form-decode query))]
           (selmer/render-file
            template
            (cond-> template-model
              query-params (assoc "query_params" query-params)))))]})))

(defn customers [_]
  (html-templated-resource
   {:template "templates/customers.html"
    :template-model
    {"customers"
     (fn [request]
       (let [rows (xt/q (:xt-node xt-node) "select customer.xt$id as id, customer.first_name, customer.last_name from customer order by customer.last_name")
             query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             q (get query-params "q")]
         (if q
           (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" q "\\E" ".*")) (str (:first_name row) (:last_name row)))) rows)
           rows
           )))}}))

(comment
  (xt/q (:xt-node xt-node) "select * from customer"))

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
             [(xt/put
               :customer
               (into (update-keys body keyword)
                     {:xt/id (next-id :customer)
                      :xt/valid-from (java.util.Date.)
                      :active true
                      }))]))
          ;; Redirect
          {:ring.response/status 302
           :ring.response/headers {"location" (var->path #'customers)}})}}
      :representations
      [^{:headers {"content-type" "text/html;charset=utf-8"}}
       (fn [req]
         (let [query-params (when-let [query (:ring.request/query req)]
                              (form-decode query))]
           (selmer/render-file
            template
            (cond-> template-model
              query-params (assoc "query_params" query-params)))))]})))

(defn ^{:uri-template "customers/{id}"} customer [{:keys [path-params]}]
  (html-templated-resource
   {:template "templates/customer.html"
    :template-model
    {"customer"
     (fn [request]
       (let [row (first (xt/q (:xt-node xt-node) (format "select customer.xt$id as id, customer.first_name, customer.last_name, customer.email, address.phone from customer, address where customer.xt$id = %s and customer.address_id = address.xt$id" (get path-params "id"))))]
         row))

     }}))

(comment
  (take 10 (xt/q (:xt-node xt-node) "select customer.xt$id as id, customer.* from customer")))

(comment
  (take 10 (xt/q (:xt-node xt-node) "select address.xt$id as id, address.* from address")))

#_(xt/q (:xt-node xt-node) "select customer.xt$id as id, customer.* from customer")
