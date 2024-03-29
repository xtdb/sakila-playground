(ns xtdb.demo.web.request-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [xtdb.demo.web.request :as req]
   [xtdb.demo.web.reap :refer [make-header-reader]]
   [xtdb.demo.web.resource :refer [map->Resource]]))

(defn test-resource []
  (map->Resource
   {:methods
    {"POST"
     {:accept ["application/x-www-form-urlencoded"]
      :max-content-length 100000
      :handler (fn [resource req] (throw (ex-info "TODO" {})))}}}))

(defn test-request
  ([method headers body-str]
   (let [req {:request-method method
              :headers headers}]
     (cond-> req
       body-str (assoc :body (java.io.ByteArrayInputStream. (.getBytes body-str)))
       true (assoc :header-reader (make-header-reader headers)))))
  ([method headers]
   (test-request method headers nil)))

(deftest request-handling-test
  (let [resource (test-resource)]

    (testing "No Content-Length header found"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"No Content-Length header found"
        (req/read-request-body
         resource
         {:request-method :post}))))

    (testing "Bad content length"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Bad content length"
        (req/read-request-body
         resource
         (test-request :post {"content-length" "ABC"})))))

    (testing "Max content length exceeded"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Payload too large"
        (req/read-request-body
         (test-resource)
         (test-request :post {"content-length" "120000"})))))

    (testing "No body in request"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"No body in request"
        (req/read-request-body
         (test-resource)
         (test-request :post {"content-length" "10"})))))

    (testing "Malformed content-type"
      (is
       (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Malformed content-type"
        (req/read-request-body
         (test-resource)
         (test-request
          :post
          {"content-length" "10"
           "content-type" "application"}
          "foo=bar")))))

    (testing "application/x-www-form-urlencoded decoded"
      (let [body "a=b&c=d"]
        (is
         (=
          {"a" "b", "c" "d"}
          (req/read-request-body
           (test-resource)
           (test-request
            :post
            {"content-length" (str (count (.getBytes body)))
             "content-type" "application/x-www-form-urlencoded"}
            body))))))))
