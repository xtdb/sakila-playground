(ns xtdb.demo.web.request
  (:require
   [ring.util.codec :refer [form-decode]]))

(defmulti read-body-bytes (fn [content-type bytes] content-type))

(defmethod read-body-bytes "application/x-www-form-urlencoded" [_ bytes]
  (form-decode (String. bytes)))

(defmethod read-body-bytes "application/octet-stream" [_ bytes]
  bytes)

(defn read-request-body
  "Check and load the representation enclosed in the request message payload."
  [resource {method :ring.request/method :as req}]

  (let [content-length
        (try
          (some->
           (get-in req [:ring.request/headers "content-length"])
           (Long/parseLong))
          (catch NumberFormatException e
            (throw
             (ex-info
              "Bad content length"
              {:ring.response/status 400}
              e))))]

    (when (nil? content-length)
      (throw
       (ex-info
        "No Content-Length header found"
        {:ring.response/status 411})))

    ;; Protects resources from PUTs that are too large. If you need to
    ;; exceed this limitation, explicitly declare :max-content-length in
    ;; your resource.
    (let [max-content-length
          (or
           (get-in resource [:methods (case method :put "PUT" :post "POST") :max-content-length])
           (Math/pow 2 24) ;;16MB
           )]
      (when (> content-length max-content-length)
        (throw
         (ex-info
          "Payload too large"
          {:ring.response/status 413}))))

    (when (and (pos? content-length) (nil? (:ring.request/body req)))
      (throw
       (ex-info
        "No body in request"
        {:ring.response/status 400})))

    (when (and
           (= method :put)
           (get-in req [:ring.request/headers "content-range"]))
      (throw
       (ex-info
        "Content-Range header not allowed on a PUT request"
        {:ring.response/status 400})))

    (assert (:header-reader req))

    (let [acceptable-content-types
          (set (get-in resource [:methods
                                 (case method :put "PUT" :post "POST")
                                 :accept]))
          parsed-request-content-type ((:header-reader req) "content-type")
          request-content-type (str (:juxt.reap.rfc7231/type parsed-request-content-type)
                                    "/"
                                    (:juxt.reap.rfc7231/subtype parsed-request-content-type))]

      (when-not (contains? acceptable-content-types request-content-type)
        (throw (ex-info (format "Unsupported media type: %s" request-content-type)
                        {:ring.response/status 415
                         :content-type request-content-type
                         :acceptable-content-types acceptable-content-types
                         :resource resource})))

      (when (pos? content-length)
        (with-open [in (:ring.request/body req)]
          (read-body-bytes request-content-type (.readNBytes in content-length)))))))
