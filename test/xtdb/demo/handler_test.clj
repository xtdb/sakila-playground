(ns xtdb.demo.handler-test
  (:require
   [clojure.test :refer [deftest is]]
   [xtdb.demo.web.handler :as handler]
   [xtdb.demo.web.locator :as locator]
   [xtdb.demo.web.protocols :refer [GET]]
   [xtdb.demo.web.resource :refer [not-found-resource]]))

(deftest handler-test
  (is (= 4 (+ 2 2)))


  )

(let [request {:ring.request/method :get
               :ring.request/path "/foo"}
      resource (locator/locate-resource request)]
  (handle-request resource request)
  )

(GET not-found-resource {:ring.request/method :get
                         :ring.request/path "/foo"})


((handler/make-full-ring2-handler
  {:locator locator/locate-resource})

 {:ring.request/method :get
  :ring.request/path "/foo"}
 )
