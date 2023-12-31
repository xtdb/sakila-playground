(ns xtdb.demo.web.handler-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.handler :as handler]
   [xtdb.demo.web.locator :as locator]))

(def handler
  (handler/make-base-ring2-handler
   {:locator locator/locate-resource}))

(deftest handler-test
  (testing "Not found"
    (is
     (=
      {:ring.response/status 404
       :ring.response/headers {"content-type" "text/plain;charset=utf-8"}
       :ring.response/body "Not found\r\n"}
      (handler
       {:ring.request/method :get
        :ring.request/path "/foo"})))))
