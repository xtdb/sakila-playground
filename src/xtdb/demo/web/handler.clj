(ns xtdb.demo.web.handler
  (:require
   [xtdb.demo.web.locator :as locator]
   [xtdb.demo.web.protocols :refer [GET HEAD POST PUT DELETE OPTIONS]]
   [xtdb.demo.web.reap :refer [make-header-reader]]))

(defn handle-request [resource req]
  (case (:ring.request/method req)
    :get (GET resource req)
    :head (HEAD resource req)
    :post (POST resource req)
    :put (PUT resource req)
    :delete (DELETE resource req)
    :options (OPTIONS resource req)
    {:ring.response/status 501}))

(defn make-base-ring2-handler
  [{:keys [locator] :or {locator locator/locate-resource}}]
  (fn [req]
    (let [resource (locator req)]
      (handle-request resource req))))

(defn wrap-add-content-length [h]
  (fn [req]
    (let [response (h req)
          body (:ring.response/body response)
          content-length (get-in response [:ring.response/headers "content-length"])]
      (if (and (string? body) (nil? content-length))
        (assoc-in response [:ring.response/headers "content-length"] (str (count (.getBytes body "utf-8"))))
        response))))

(defn wrap-add-header-reader [h]
  (fn [req]
    (h (assoc req :header-reader (make-header-reader (:ring.request/headers req))))))

(defn make-full-ring2-handler
  ([opts]
   (-> (make-base-ring2-handler opts)
       (wrap-add-content-length)
       (wrap-add-header-reader)))
  ([] (make-full-ring2-handler {})))
