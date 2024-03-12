(ns xtdb.demo.web.handler
  (:require
   [xtdb.demo.web.protocols :refer [GET HEAD POST PUT DELETE OPTIONS]]
   [xtdb.demo.web.reap :refer [make-header-reader]]))

(defn handle-request [resource req]
  (case (:request-method req)
    :get (GET resource req)
    :head (HEAD resource req)
    :post (POST resource req)
    :put (PUT resource req)
    :delete (DELETE resource req)
    :options (OPTIONS resource req)
    {:status 501}))

(defn make-base-ring-handler
  [{:keys [locator]}]
  (assert locator)
  (fn [req]
    (let [resource (locator req)]
      (handle-request resource req))))

(defn wrap-add-content-length [h]
  (fn [req]
    (let [response (h req)
          body (:body response)
          content-length (get-in response [:headers "content-length"])]
      (if (and (string? body) (nil? content-length))
        (assoc-in response [:headers "content-length"] (str (count (.getBytes body "utf-8"))))
        response))))

(defn wrap-add-header-reader [h]
  (fn [req]
    (h (assoc req :header-reader (make-header-reader (:headers req))))))

(defn make-full-ring-handler
  ([opts]
   (-> (make-base-ring-handler opts)
       (wrap-add-content-length)
       (wrap-add-header-reader)))
  ([] (make-full-ring-handler {})))
