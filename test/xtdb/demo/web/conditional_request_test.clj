(ns xtdb.demo.web.conditional-request-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.methods :as methods]
   [xtdb.demo.web.resource :refer [map->Resource]]
   [juxt.reap.encoders :refer [format-http-date]])
  (:import
   (java.time Instant)
   (java.util Date)))

(deftest last-modified-test
  (is
   (=
    {:ring.response/status 201,
     :ring.response/headers
     {"content-type" "text/plain",
      "last-modified" "Mon, 1 Jan 2024 10:00:00 GMT"},
     :ring.response/body "foo"}

    (let [resource
          (map->Resource
           {:representations
            [^{"content-type" "text/plain"}
             (fn [_]
               {:ring.response/status 201
                :ring.response/headers {"last-modified" (format-http-date (java.util.Date/from (java.time.Instant/parse "2024-01-01T10:00:00Z")))}
                :ring.response/body "foo"})]})]
      (methods/GET resource {:ring.request/method :get})))))


(let [request {:ring.request/method :get}
      resource
      (fn [req]
        (map->Resource
         {:representations
          [^{"content-type" "text/plain"
             "last-modified"
             (-> "2024-01-01T10:00:00Z"
                 Instant/parse
                 Date/from
                 format-http-date)}
           (fn [_]
             {:ring.response/body "foo"})]}))]

  (methods/GET (resource request) request))
