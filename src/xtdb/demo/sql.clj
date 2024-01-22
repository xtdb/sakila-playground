(ns xtdb.demo.sql
  {:web-context "/sql/"}
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [xtdb.demo.web.resource :refer [map->Resource]]
            [xtdb.demo.db :refer [q]]
            [ring.util.codec :refer [form-encode form-decode]])
  (:import (java.io File PushbackReader)
           (java.time Instant)))

(defn parse-comment-line [comment-line]
  (let [[kw arg] (str/split (subs comment-line 3) #"\s+" 2)]
    [(edn/read-string kw) (edn/read-string arg)]))

(defn parse-sql-file [sql-type file]
  (let [file-name (.getName (io/file file))
        sql-lines (str/split-lines (slurp file))
        comment-line? #(str/starts-with? % "-- :")
        comment-lines (filter comment-line? sql-lines)
        comment-opts (into {} (map parse-comment-line comment-lines))
        sql-string (str/join "\n" (remove comment-line? sql-lines))]
    (merge
      comment-opts
      {:sql-type sql-type
       :file-name file-name
       :sql-string (str/trim sql-string)})))

(comment

  (parse-sql-file :query "sql/queries/customers.sql")
  (parse-sql-file :query "sql/queries/customer-rentals.sql")

  )

(defn head [title]
  [:head
   [:title title]
   [:link {:rel "icon" :href "data:,"}]
   [:link {:rel "stylesheet" :href "/static/codemirror.css"}]
   [:link {:rel "stylesheet" :href "/static/missing.css.css"}]
   [:link {:rel "stylesheet" :href "/static/screen.css"}]
   [:script {:src "/static/htmx.org.js"}]
   [:script {:src "/static/hyperscript.js"}]
   [:script {:src "https://cdn.jsdelivr.net/npm/vega@5.25.0"}]
   [:script {:src "https://cdn.jsdelivr.net/npm/vega-lite@5.16.3"}]
   [:script {:src "https://cdn.jsdelivr.net/npm/vega-embed@6.22.2"}]
   [:script {:src "/static/codemirror.js"}]
   [:script {:src "/static/sql.js"}]
   [:script {:src "/static/sakila.js"}]])

(defn body [content]
  [:body {:style "margin: 6pt 6pt"}
   content
   [:footer {:style "text-align: left; padding: 0; margin: 0"}
    [:div {:style "font-size: 80%; color: #ccc"}
     [:p "XTDBv2 Demonstrator. Copyright © 2024, JUXT LTD. "
      [:a {:href "/"} "Home"] " | "
      [:a {:href "https://github.com/xtdb/sakila-playground"} "GitHub"]
      [:br] "Uses the " [:a {:href "https://www.jooq.org/sakila"} "Sakila"] " data set. Copyright © 2009 - 2024 by Data Geekery™ GmbH. All rights reserved."]
     [:div {:style "margin-top: 24pt"}
      [:img {:style "width: 60px" :src "/static/juxt.svg"}]]]]])

(defn page [title content]
  [:html
   (head title)
   (body content)])

(defn page-response [title content]
  {:ring.response/body (str (h/html (page title content)))})

(defn query-url
  ([file-name] (query-url file-name {}))
  ([file-name params]
   (format "/sql/queries/%s%s"
           file-name
           (if (empty? params)
             ""
             (str "?" (form-encode params))))))

(defn ^{:web-path "queries"} queries-resource [{}]
  (map->Resource
    {:representations
     [^{"content-type" "text/html;charset=utf-8"}
      (fn [_]
        (page-response
          "queries"
          [:div
           [:ul
            (for [file (file-seq (io/file "sql/queries"))
                  :when (and (.isFile file) (str/ends-with? (str file) ".sql"))]
              [:li [:a {:href (query-url (.getName file))} (.getName file)]])]]))]}))

(defn sql-editor [{:keys [sql-string]}]
  (let [id (str (random-uuid))]
    [:div
     [:textarea {:id id, :name "sql"} sql-string]
     [:script (h/raw (format "Sakila.initialiseSqlEditor(document.getElementById('%s'), document.getElementById('query-form'));" id))]]))

(defn sort-cols [result-set-cols col-order]
  (let [index-of (into {} (map-indexed (fn [i col] [col i]) col-order))]
    (sort-by (fn [col] [(index-of col Long/MAX_VALUE) (= :xt/valid-from col) col]) result-set-cols)))

(comment
  (sort-cols

    [:d :a :b :c]
    [:a :b :c]

    )
  )

(defn get-ref [explicit-refs col]
  (or (get explicit-refs col)
      (when (str/ends-with? (name col) "-id")
        (str (str/join "-" (butlast (str/split (name col) #"-"))) ".sql"))))

(defn evaluate-query [{:keys [file-name, sql-string refs col-order]} args]
  (try
    (let [rs (q sql-string {:args args})
          cols (sort-cols (keys (first rs)) col-order)]
      (if (empty? rs)
        [:div "No results"]
        [:table
         [:thead
          (for [col cols]
            [:th (name col)])]
         [:tbody
          (for [row rs]
            [:tr
             (for [col cols
                   :let [value (col row)]]
               (if-some [query-file (get-ref refs col)]
                 [:td [:a {:href (query-url query-file {:id value})} value]]
                 [:td value]))])]]))
    (catch Throwable t
      [:div
       [:h2 "Exception!"]
       [:pre (with-out-str (binding [*err* *out*] (clojure.repl/pst t)))]])))

(defn satisfy-params [query req]
  (let [{:keys [params, defaults]} query
        {query-params :ring.request/query} req
        query-params (some-> query-params form-decode)]
    (->> (for [[param t] params]
           [param (case t
                    :str (get query-params (name param))
                    :long (try
                            (parse-long (get query-params (name param)))
                            (catch Throwable _ nil)))])
         (into {})
         (merge-with (fn [a b] (if (nil? b) a b)) defaults))))

(defn satisfy-query-args [query req]
  (let [{:keys [param-order]} query
        pmap (satisfy-params query req)]
    (mapv pmap param-order)))

(defn param-label [param t]
  [:label {:style "display: inline-block; vertical-align:middle; margin-right:10px" :for (name param)} (name param)])

(defn param-input [param t v]
  [:input {:style "display: inline-block; vertical-align:middle; border-radius:5px; padding:8px; border: 1px solid #ccc;" :name (name param), :value v}])

(defn parameter-view [{:keys [params] :as query} req]
  (when (seq params)
    (let [satisfied-params (satisfy-params query req)]
      [:div
       [:h2 "Parameters"]
       (for [[param t] params]
         [:div {:style "padding:10px"}
          (param-label param t)
          (param-input param t (satisfied-params param))])])))

(defn specialise-query [query req]
  (let [{user-sql "sql"} (some-> req :ring.request/query form-decode)
        user-sql (if (sequential? user-sql) (first user-sql) user-sql)]
    (if user-sql
      ;; xt does not like carriage returns for some reason
      (assoc query :sql-string (str/replace user-sql "\r" ""))
      query)))

(defn ^{:uri-template "queries/{file}"} query-file-resource [{:keys [path-params]}]
  (let [{:strs [file]} path-params
        {:keys [file-name, title, desc] :as query} (parse-sql-file :query (io/file "sql" "queries" file))]
    (map->Resource
      {:representations
       [^{"content-type" "text/html;charset=utf-8"}
        (fn [req]
          (let [query (specialise-query query req)]
            (page-response
              (str "queries/" file-name)
              [:div
               [:h1 [:a {:href "/sql/queries"} "queries"] "/" file-name]
               (when title [:h2 title])
               (when desc [:pre desc])
               [:form {:id "query-form",
                       :hx-trigger "submit"
                       :hx-get "",
                       :hx-target "#query-results",
                       :hx-select "#query-results"}
                (sql-editor query)
                (parameter-view query req)
                [:div {:id "query-results"}
                 (evaluate-query query (satisfy-query-args query req))]]])))]})))

(comment

  (clojure.java.browse/browse-url "http://localhost:3000/sql/queries")
  (clojure.java.browse/browse-url "http://localhost:3000/sql/queries/customers.sql")

  )

(defn infer-schema [edn-file]
  (with-open [in (io/input-stream edn-file)
              rdr (io/reader in)
              pb-rdr (PushbackReader. rdr)]
    (let [m (edn/read {:readers {'time/instant #(Instant/parse %)}} pb-rdr)]
      (into {}
            (for [[k v] m
                  :when (not= :xt/valid-from k)]
              [k {:example v
                  :ref (when (str/ends-with? (name k) "_id") (str/join "_" (butlast (str/split (name k) #"_"))))
                  :data-type (cond
                               (boolean? v) :bit
                               (int? v) :long
                               (double? v) :double
                               (inst? v) :datetime
                               (string? v) :varchar
                               (set? v) :set
                               :else :any)}])))))

(comment
  (infer-schema (io/resource "sakila/actor.edn"))
  )

;; could use this data for auto-complete
(def schema
  (->> (for [seed-file ["sakila/payment.edn"
                        "sakila/country.edn"
                        "sakila/city.edn"
                        "sakila/address.edn"
                        "sakila/inventory.edn"
                        "sakila/store.edn"
                        "sakila/rental.edn"
                        "sakila/category.edn"
                        "sakila/staff.edn"
                        "sakila/film_actor.edn"
                        "sakila/language.edn"
                        "sakila/film.edn"
                        "sakila/film_category.edn"
                        "sakila/customer.edn"
                        "sakila/actor.edn"]
             :let [[_ table-name] (re-find #"sakila/(.+)\.edn" seed-file)
                   table-schema (infer-schema (io/resource seed-file))]]
         [table-name table-schema])
       (into {})))

(defn ^{:web-path "schema"} schema-resource [_]
  (map->Resource
    {:representations
     [^{"content-type" "text/html;charset=utf-8"}
      (fn [req]
        (page-response
          "Schema"
          [:div
           [:h1 "Schema"]
           (for [[table-name table-schema] (sort-by key schema)]
             [:div {:style "display:inline-block; margin:10px; padding:10px; border:solid 2px; border-radius:5px"}
              [:h2 table-name]
              [:table {:style "table-layout:auto;"}
               [:thead [:th "column"] [:th "data type"] [:th "example"]]
               [:tbody
                (for [[col {:keys [data-type, ref, example]}] (sort-by key table-schema)]
                  [:tr
                   [:td (str col)]
                   [:td
                    (str/upper-case (name data-type))
                    (when ref (format " REF(%s, xt$id)" ref))]
                   [:td example]])]]])]))]}))

(comment

  (clojure.java.browse/browse-url "http://localhost:3000/sql/schema")
  )

(comment
  ;; generate table queries
  (doseq [table (keys schema)
          :let [solo-content
                ["-- :params {:id :long}"
                 "-- :param-order [:id]"
                 "SELECT *"
                 (format "FROM %s" table)
                 (format "WHERE %s.xt$id = ?" table)]
                list-content
                ["SELECT *"
                 (format "FROM %s" table)
                 (format "ORDER BY %s.xt$id" table)
                 "LIMIT 100"]]]
    (spit (format "sql/queries/%s.sql" table) (str/join "\n" solo-content))
    (spit (format "sql/queries/%s_list.sql" table) (str/join "\n" list-content)))

  )