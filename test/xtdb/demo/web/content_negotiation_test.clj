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
    {:ring.response/status 200,
     :ring.response/headers {"content-type" "text/html"},
     :ring.response/body "<h1>Hello</h1>"}

    (let [resource
          (map->Resource
           {:representations
            [^{"content-type" "text/plain"}
             (fn [_] {:ring.response/body "Hello"})

             ^{"content-type" "text/html"}
             (fn [_] {:ring.response/body "<h1>Hello</h1>"})]})]

      (methods/GET
       resource
       {:ring.request/method :get
        :ring.request/headers {"accept" "text/html"}})))))
