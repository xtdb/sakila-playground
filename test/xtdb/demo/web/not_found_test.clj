(ns xtdb.demo.web.not-found-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.handler :as handler]
   [xtdb.demo.web.locator :as locator]))

(def handler
  (handler/make-base-ring-handler
   {:locator (fn [req]
               (locator/find-resource
                []                      ; resource tree
                (:uri req)))}))

(deftest handler-test
  (testing "Not found"
    (is
     (=
      {:status 404
       :headers {"content-type" "text/plain;charset=utf-8"}
       :body "Not found\r\n"}
      (handler
       {:request-method :get
        :uri "/foo"})))))


#_(handler
 {:method :get
  :uri "/foo"})
