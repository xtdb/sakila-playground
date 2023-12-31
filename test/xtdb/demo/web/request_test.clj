(ns xtdb.demo.web.request-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.request :as req]
   [xtdb.demo.web.resource :refer [map->Resource]]))

(defn test-resource []
  (map->Resource
   {
    :methods
    {"POST"
     {:handler (fn [req] (throw (ex-info "TODO" {})))
      :max-content-length 100000
      }}

    }))



(deftest request-handling-test
  (let [resource (test-resource)]

    (testing "No Content-Length header found"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo

        #"No Content-Length header found"

        (req/receive-representation
         resource
         {:ring.request/method :post}))))

    (testing "Bad content length"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo

        #"Bad content length"

        (req/receive-representation
         resource
         {:ring.request/method :post
          :ring.request/headers {"content-length" "ABC"}}))))

    (testing "Max content length exceeded"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Payload too large"
        (req/receive-representation
         (test-resource
          )
         {:ring.request/method :post
          :ring.request/headers {"content-length" "120000"}})))
      )

    (testing "No body in request"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"No body in request"
        (req/receive-representation
         (test-resource)
         {:ring.request/method :post
          :ring.request/headers {"content-length" "10"}}))))))
