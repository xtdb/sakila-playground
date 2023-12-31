;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.web.reap
  (:require
   [juxt.reap.regex :as re]
   [juxt.reap.decoders.rfc7231 :as rfc7231]))

(def content-type-decoder (rfc7231/content-type {}))

(defn make-header-reader [headers]
  (memoize
   (fn [header]
     (when-let [v (get headers header)]
       (case header
         "content-type"
         (or (content-type-decoder (re/input v))
             (throw
              (ex-info
               "Malformed content-type"
               {:ring.request/status 400
                :input v}))))))))
