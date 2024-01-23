(ns xtdb.demo.web.conditional
  (:require
   [juxt.reap.decoders :as reap]
   [juxt.reap.rfc7231 :as-alias rfc7231]
   [juxt.reap.rfc7232 :as rfc7232]))

(defn evaluate-if-match!
  "Evaluate an If-None-Match precondition header field in the context of a
  resource. If the precondition is found to be false, an exception is thrown
  with ex-data containing the proper response."
  [selected-representation-metadata empty-current-representations? req]
  ;; (All quotes in this function's comments are from Section 3.2, RFC 7232,
  ;; unless otherwise stated).
  (when-let [header-field (reap/if-match (get-in req [:ring.request/headers "if-match"]))]
    (cond
      ;; "If the field-value is '*' …"
      (and (map? header-field)
           (::rfc7232/wildcard header-field)
           empty-current-representations?)
      ;; "… the condition is false if the origin server does not have a current
      ;; representation for the target resource."
      (throw
       (ex-info
        "If-Match precondition failed"
        {:ring.response/status 412}))

      (sequential? header-field)
      (let [rep-etag (some-> (get selected-representation-metadata "etag") reap/entity-tag)
            matches (for [etag header-field
                          ;; "An origin server MUST use the strong comparison function
                          ;; when comparing entity-tags"
                          :when (rfc7232/strong-compare-match? etag rep-etag)]
                      etag)]
        (when-not (seq matches)
          ;; TODO: "unless it can be determined that the state-changing
          ;; request has already succeeded (see Section 3.1)"
          (throw
           (ex-info
            "No strong matches between if-match and current representations"
            {:ring.response/status 412})))))))

;; TODO: See Section 4.1, RFC 7232:
;;
(defn evaluate-if-none-match!
  "Evaluate an If-None-Match precondition header field in the context of a
  resource and, when applicable, the representation metadata of the selected
  representation. If the precondition is found to be false, an exception is
  thrown with ex-data containing the proper response."
  [selected-representation req]
  ;; (All quotes in this function's comments are from Section 3.2, RFC 7232,
  ;; unless otherwise stated).
  (let [header-field (reap/if-none-match (get-in req [:ring.request/headers "if-none-match"]))]
    (cond
      (sequential? header-field)
      (when-let [rep-etag (some-> (get selected-representation :juxt.http/etag) reap/entity-tag)]
        ;; "If the field-value is a list of entity-tags, the condition is false
        ;; if one of the listed tags match the entity-tag of the selected
        ;; representation."
        (doseq [etag header-field]
          ;; "A recipient MUST use the weak comparison function when comparing
          ;; entity-tags …"
          (when (rfc7232/weak-compare-match? etag rep-etag)
            (throw
             (if (#{:get :head} (:ring.request/method req))
               (ex-info
                "Not modified"
                {:ring.response/status 304
                 ::matching-entity-tag etag})
               ;; "… or 412 (Precondition Failed) status code for all other
               ;; request methods."
               (ex-info
                "If-None-Match precondition failed"
                {:ring.response/status 412
                 ::matching-entity-tag etag}))))))

      ;; "If-None-Match can also be used with a value of '*' …"
      (and (map? header-field) (::rfc7232/wildcard header-field))
      ;; "… the condition is false if the origin server has a current
      ;; representation for the target resource."
      (when selected-representation
        (throw
         (ex-info
          "At least one representation already exists for this resource"
          {:juxt.site/request-context
           (assoc
            req
            :ring.response/status
            (if (#{:get :head} (:ring.request/method req))
              ;; "the origin server MUST respond with either a) the 304 (Not
              ;; Modified) status code if the request method is GET or HEAD
              ;; …"
              304
              ;; "… or 412 (Precondition Failed) status code for all other
              ;; request methods."
              412))}))))))

(defn evaluate-if-unmodified-since! [selected-representation request]
  (let [if-unmodified-since-date
        (-> request
            (get-in [:ring.request/headers "if-unmodified-since"])
            reap/http-date
            ::rfc7231/date)]
    (when (.isAfter
           (.toInstant (get selected-representation :juxt.http/last-modified (java.util.Date.)))
           (.toInstant if-unmodified-since-date))
      (throw
       (ex-info
        "Precondition failed"
        {:ring.response/status 304})))))

(defn evaluate-if-modified-since! [representation-metadata req]
  (when-let [last-modified (some-> representation-metadata (get "last-modified")
                                    reap/http-date
                                    :juxt.reap.rfc7231/date)]
    (let [if-modified-since-date
          (-> req
              (get-in [:ring.request/headers "if-modified-since"])
              reap/http-date
              :juxt.reap.rfc7231/date)]

      ;; todo fix this, it throws all the time and is not handled
      #_(when-not (.isAfter
                 (.toInstant last-modified)
                 (.toInstant if-modified-since-date))
        (throw
         (ex-info
          "Not modified"
          {:ring.response/status 304}))))))

(defn evaluate-if-unmodified-since! [selected-representation req]
  (let [if-unmodified-since-date
        (-> req
            (get-in [:ring.request/headers "if-unmodified-since"])
            reap/http-date
            ::rfc7231/date)]
    (when (.isAfter
           (.toInstant (get selected-representation :juxt.http/last-modified (java.util.Date.)))
           (.toInstant if-unmodified-since-date))
      (throw
       (ex-info
        "Precondition failed"
        {:juxt.site/request-context (assoc req :ring.resposne/status 304)})))))

(defn evaluate-preconditions!
  "Implementation of Section 6 of RFC 7232."
  [selected-representation-metadata request]

  ;; "… a server MUST ignore the conditional request header fields … when
  ;; received with a request method that does not involve the selection or
  ;; modification of a selected representation, such as CONNECT, OPTIONS, or
  ;; TRACE." -- Section 5, RFC 7232
  (when (not (#{:connect :options :trace} (:ring.request/method request)))

    (if (get-in request [:ring.request/headers "if-match"])
      ;; Step 1
      (evaluate-if-match! selected-representation-metadata request)
      ;; Step 2
      (when (get-in request [:ring.request/headers "if-unmodified-since"])
        (evaluate-if-unmodified-since! selected-representation-metadata request)))
    ;; Step 3
    (if (get-in request [:ring.request/headers "if-none-match"])
      (evaluate-if-none-match! selected-representation-metadata request)
      ;; Step 4, else branch: if-none-match is not present
      (when (#{:get :head} (:ring.request/method request))
        (when (get-in request [:ring.request/headers "if-modified-since"])
          (evaluate-if-modified-since! selected-representation-metadata request))))
    ;; (Step 5 is handled elsewhere)
    ))
