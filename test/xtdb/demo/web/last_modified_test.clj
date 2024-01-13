(ns xtdb.demo.web.last-modified-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.methods :as methods]
   [xtdb.demo.web.resource :refer [map->Resource]]
   [juxt.reap.encoders :refer [format-http-date]]))

(deftest set-headers-test
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
            [^{:headers {"content-type" "text/plain"}}
             (fn [_]
               {:status 201
                :headers {"last-modified" (format-http-date (java.util.Date/from (java.time.Instant/parse "2024-01-01T10:00:00Z")))}
                :body "foo"})]})]
      (methods/GET resource {:ring.request/method :get})))))
