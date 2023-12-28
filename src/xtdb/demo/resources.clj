;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.resources
  {:web-context "/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.demo.web.html :as html]
   [xtdb.demo.db :refer [xt-node]]
   [hiccup2.core :as h]
   [xtdb.api :as xt]))

(def hello
  (let [state (atom {:greeting "Hello"})]
    (map->Resource
     {:representations
      [^{:headers {"content-type" "text/html;charset=utf-8"}}
       (fn [] (format "<h1>%s World!</h1>\r\n" (:greeting @state)))
       ]})))

(def select-films
  "select film.title, film.description from film order by film.title limit 20")

(def films
  (html-resource
   (fn []
     (->
      (xt/q xt-node select-films)
      (html/html-table {:rowspecs [:title :description]})
      (h/html)
      (str "\r\n")))))

(def test-page
  (html-templated-resource
   {:template "templates/basic.html"
    :template-model
    {"content"
     (fn []
       (->
        (xt/q xt-node select-films)
        (html/html-table {:rowspecs [:title :description]})
        (h/html)
        (str "\r\n")))}}))
