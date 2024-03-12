(ns xtdb.demo.web.content-negotiation-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.methods :as methods]
   [xtdb.demo.web.resource :refer [map->Resource]]
   [juxt.reap.encoders :refer [format-http-date]]
   [juxt.pick.ring :refer [pick]]))

(deftest content-negotiation-test
  (is
   (=
    {:status 200,
     :headers {"content-type" "text/html"},
     :body "<h1>Hello</h1>"}

    (let [resource
          (map->Resource
           {:representations
            [^{"content-type" "text/plain"}
             (fn [_] {:body "Hello"})

             ^{"content-type" "text/html"}
             (fn [_] {:body "<h1>Hello</h1>"})]})]

      (methods/GET
       resource
       {:request-method :get
        :headers {"accept" "text/html"}})))))
