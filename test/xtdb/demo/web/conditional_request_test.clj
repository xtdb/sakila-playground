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
    {:status 201,
     :headers
     {"content-type" "text/plain",
      "last-modified" "Mon, 1 Jan 2024 10:00:00 GMT"},
     :body "foo"}

    (let [resource
          (map->Resource
           {:representations
            [^{"content-type" "text/plain"}
             (fn [_]
               {:status 201
                :headers {"last-modified" (format-http-date (java.util.Date/from (java.time.Instant/parse "2024-01-01T10:00:00Z")))}
                :body "foo"})]})]
      (methods/GET resource {:request-method :get})))))

(deftest not-modified-test
  (testing "more recent if-modified-since yields not modified"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Not modified"
         (let [resource
               (let [state (atom {:last-modified (-> "2024-01-10T00:00:00Z" Instant/parse)})]
                 (fn [_]
                   (map->Resource
                    {:representations
                     [^{"content-type" "text/plain"
                        "last-modified"
                        (format-http-date (Date/from (:last-modified @state)))}
                      (fn [_]
                        {:body "foo"})]})))]

           (methods/GET
            (resource {})
            {:request-method :get
             :headers
             {"if-modified-since"
              (-> "2024-01-20T00:00:00Z" Instant/parse Date/from format-http-date)}})))))

  (testing "no last-modified metadata yields OK"
    (is
     (=
      200
      (:status
       (let [resource
             (fn [_]
               (map->Resource
                {:representations
                 [^{"content-type" "text/plain"}
                  (fn [_]
                    {:body "foo"})]}))]

         (methods/GET
          (resource {})
          {:request-method :get
           :headers
           {"if-modified-since"
            (-> "2024-01-20T00:00:00Z" Instant/parse Date/from format-http-date)}})))))))
