;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.web.request)

(defn receive-representation
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
    ;; exceed this limitation, explicitly declare ::spin/max-content-length in
    ;; your resource.
    (let [max-content-length
          (or
           (case method
             :put (get-in resource [:methods "PUT" :max-content-length])
             :post (get-in resource [:methods "POST" :max-content-length]))
           (Math/pow 2 24) ;;16MB
           )]
      (when (> content-length max-content-length)
        (throw
         (ex-info
          "Payload too large"
          {:ring.response/status 413
           :juxt.site/request-context req}))))

    (when (and (pos? content-length) (nil? (:ring.request/body req)))
      (throw
       (ex-info
        "No body in request"
        {:ring.response/status 400
         :juxt.site/request-context req})))

    #_(let [decoded-representation
            (decode-maybe

             ;; See Section 3.1.1.5, RFC 7231 as to why content-type defaults
             ;; to application/octet-stream
             (cond-> {:juxt.http/content-type "application/octet-stream"}
               (contains? (:ring.request/headers req) "content-type")
               (assoc :juxt.http/content-type (get-in req [:ring.request/headers "content-type"]))

               (contains? (:ring.request/headers req) "content-encoding")
               (assoc :juxt.http/content-encoding (get-in req [:ring.request/headers "content-encoding"]))

               (contains? (:ring.request/headers req) "content-language")
               (assoc :juxt.http/content-language (get-in req [:ring.request/headers "content-language"]))))]

        ;; TODO: Someday there should be a functions that could be specified to
        ;; handle conversions as described in RFC 7231 Section 4.3.4

        ;; TODO: Add tests for content type (and other axes of) acceptance of PUT
        ;; and POST

        (when-let [acceptable
                   (get-in resource [:juxt.site/methods method :juxt.site/acceptable])]

          (let [prefs (headers->decoded-preferences acceptable)
                request-rep (rate-representation prefs decoded-representation)]

            (when (or (get prefs "accept") (get prefs "accept-charset"))
              (cond
                (= (:juxt.pick/content-type-qvalue request-rep) 0.0)
                (throw
                 (ex-info
                  "The content-type of the request payload is not supported by the resource"
                  {:ring.response/status 415
                   ::acceptable acceptable
                   ::content-type (get request-rep "content-type")
                   :juxt.site/request-context req}))

                (and
                 (= "text" (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/type]))
                 (get prefs "accept-charset")
                 (not (contains? (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map]) "charset")))
                (throw
                 (ex-info
                  "The Content-Type header in the request is a text type and is required to specify its charset as a media-type parameter"
                  {:ring.response/status 415
                   ::acceptable acceptable
                   ::content-type (get request-rep "content-type")
                   :juxt.site/request-context req}))

                (= (:juxt.pick/charset-qvalue request-rep) 0.0)
                (throw
                 (ex-info
                  "The charset of the Content-Type header in the request is not supported by the resource"
                  {:ring.response/status 415
                   ::acceptable acceptable
                   ::content-type (get request-rep "content-type")
                   :juxt.site/request-context req}))))

            (when (get prefs "accept-encoding")
              (cond
                (= (:juxt.pick/content-encoding-qvalue request-rep) 0.0)
                (throw
                 (ex-info
                  "The content-encoding in the request is not supported by the resource"
                  {:ring.response/status 409
                   ::acceptable acceptable
                   ::content-encoding (get-in req [:ring.request/headers "content-encoding"] "identity")
                   :juxt.site/request-context req}))))

            (when (get prefs "accept-language")
              (cond
                (not (contains? (:ring.response/headers req) "content-language"))
                (throw
                 (ex-info
                  "Request must contain Content-Language header"
                  {:ring.response/status 409
                   ::acceptable acceptable
                   ::content-language (get-in req [:ring.request/headers "content-language"])
                   :juxt.site/request-context req}))

                (= (:juxt.pick/content-language-qvalue request-rep) 0.0)
                (throw
                 (ex-info
                  "The content-language in the request is not supported by the resource"
                  {:ring.response/status 415
                   ::acceptable acceptable
                   ::content-language (get-in req [:ring.request/headers "content-language"])
                   :juxt.site/request-context req}))))))

        (when (get-in req [:ring.request/headers "content-range"])
          (throw
           (ex-info
            "Content-Range header not allowed on a PUT request"
            {:ring.response/status 400
             :juxt.site/request-context req})))

        (if (pos? content-length)
          (with-open [in (:ring.request/body req)]
            (let [body (.readNBytes in content-length)
                  content-type (:juxt.reap.alpha.rfc7231/content-type decoded-representation)]

              (assoc
               req
               :juxt.site/received-representation
               (merge
                decoded-representation
                {:juxt.http/content-length content-length
                 :juxt.http/last-modified start-date}

                (if (and
                     (= (:juxt.reap.alpha.rfc7231/type content-type) "text")
                     (nil? (get decoded-representation :juxt.http/content-encoding)))
                  (let [charset
                        (get-in decoded-representation
                                [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map "charset"])]
                    (merge
                     {:juxt.http/content (new String body (or charset "utf-8"))}
                     (when charset {:juxt.http/charset charset})))

                  {:juxt.http/body body})))))
          ;; Zero content-length, return request
          req))))
