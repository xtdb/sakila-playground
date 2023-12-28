(ns xtdb.demo.resources
  {:web-context "/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node]]
   [ring.util.codec :refer [form-decode]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]
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
      (xt/q xt-node select-films)
      (html/html-table {:rowspecs [:title :description]})
      (h/html)
      (str "\r\n")))))

(defn films [_]
  (html-templated-resource
   {:template "templates/films.html"
    :template-model
    {"films"
     (fn [request]
       (let [rows (xt/q xt-node select-films)
             query-params (when-let [query (:ring.request/query request)]
                            (form-decode query))
             q (get query-params "q")]
         (if q
           (filter (fn [row] (re-matches (re-pattern (str "(?i)" ".*" "\\Q" q "\\E" ".*")) (str (:title row) (:description row)))) rows)
           rows
           )))}}))

(defn ^{:web-path "films/new"} films-new [_]
  (let [template "templates/new.html"
        template-model {}]
    (map->Resource
     {:representations
      [^{:headers {"content-type" "text/html;charset=utf-8"}}
       (fn [req]
         (let [query-params (when-let [query (:ring.request/query req)]
                              (form-decode query))]
           (selmer/render-file
            template
            (cond-> template-model
              query-params (assoc "query_params" query-params)))))]})))

;;(xt/q xt-node "select * from film")
