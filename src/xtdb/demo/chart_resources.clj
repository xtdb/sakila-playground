;; Copyright © 2024, JUXT LTD.

(ns xtdb.demo.chart-resources
  {:web-context "/charting/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource templated-responder]]
   [xtdb.demo.db :refer [xt-node next-id q]]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [xtdb.api :as xt])
  )

(def rentals-per-category-query
  "WITH rental_categories AS
        (SELECT film_category.category_id as category_id,
                count(*) as films_rented
         FROM rental
         LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
         LEFT JOIN film_category ON inventory.film_id = film_category.film_id
         WHERE rental.return_date IS NULL
         GROUP BY film_category.category_id)
    SELECT category.name AS category_name,
           rental_categories.category_id,
           rental_categories.films_rented
    FROM rental_categories
    LEFT JOIN category ON rental_categories.category_id = category.xt$id
    ORDER BY category.name ASC")




(defn bar-graph [_]
  (let [rows (xt/q (:xt-node xt-node) rentals-per-category-query)]
    (map->Resource
     {:representations
      [^{"content-type" "application/json"}
       (fn [req] {:body
                  (json/write-str
                   {"width" 500,
                    "height" 410,
                    "autosize" "fit",
                    "padding" 5,
                    "marks"
                    [{"type" "rect",
                      "from" {"data" "directors"},
                      "encode"
                      {"update"
                       {"x" {"scale" "x", "value" 0},
                        "x2" {"scale" "x", "field" "Gross"},
                        "y" {"scale" "y", "field" "Director"},
                        "height" {"scale" "y", "band" 1}}}}],
                    "scales"
                    [{"name" "x",
                      "type" "linear",
                      "domain" {"data" "directors", "field" "Gross"},
                      "range" "width",
                      "nice" true}
                     {"name" "y",
                      "type" "band",
                      "domain"
                      {"data" "directors",
                       "field" "Director",
                       "sort" {"op" "max", "field" "Gross", "order" "descending"}},
                      "range" "height",
                      "padding" 0.1}],
                    "axes"
                    [{"scale" "x", "orient" "bottom", "format" "$,d", "tickCount" 5}
                     {"scale" "y", "orient" "left"}],
                    "signals"
                    [{"name" "k",
                      "value" 20,
                      "bind" {"input" "range", "min" 10, "max" 30, "step" 1}}
                     {"name" "op",
                      "value" "average",
                      "bind" {"input" "select", "options" ["average" "median" "sum"]}}
                     {"name" "label",
                      "value" {"average" "Average", "median" "Median", "sum" "Total"}}],
                    "$schema" "https://vega.github.io/schema/vega/v5.json",
                    "title"
                    {"text"
                     {"signal" "'Top Directors by ' + label[op] + ' Worldwide Gross'"},
                     "anchor" "start",
                     "frame" "group"},
                    "data"
                    [{"name" "directors",
                      "url" "/static/movies.json",
                      "transform"
                      [{"type" "filter",
                        "expr"
                        "datum.Director != null && datum['Worldwide Gross'] != null"}
                       {"type" "aggregate",
                        "groupby" ["Director"],
                        "ops" [{"signal" "op"}],
                        "fields" ["Worldwide Gross"],
                        "as" ["Gross"]}
                       {"type" "window",
                        "sort" {"field" "Gross", "order" "descending"},
                        "ops" ["row_number"],
                        "as" ["rank"]}
                       {"type" "filter", "expr" "datum.rank <= k"}]}],
                    "description" "A top-k bar chart ranking film directors by revenue."})
                  })]})))
