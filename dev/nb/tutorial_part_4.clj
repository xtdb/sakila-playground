(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.tutorial-part-4
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [xtdb.api :as xt]
            [xtdb.node :as xt-node])
  (:import (java.time Instant ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)))

{::clerk/visibility {:code :hide, :result :hide}}

(clerk/add-viewers!
 {:pred #(instance? ZonedDateTime %)
  :transform-fn (clerk/update-val #(.format % DateTimeFormatter/ISO_LOCAL_DATE_TIME))})

(defonce xtdb nil)

(defonce current-time nil)

(defn reset []
  (alter-var-root #'current-time (constantly nil))
  (alter-var-root
    #'xtdb
    (fn [n]
      (when n (.close n))
      (xt-node/start-node {})))
  (clerk/md ""))

(defn e [& sql]
  (let [sql (str/join "\n" sql)]
    (xt/submit-tx xtdb [[:sql sql]] {:system-time current-time})
    (clerk/html
     [:div
      [:div (clerk/code {::clerk/render-opts {:language "sql"}} sql)]])))

(defn table [rs]
  (if (seq rs)
    (clerk/table rs)
    (clerk/table [{"" "no results"}])))

(defn q [& sql]
  (let [sql (str/join "\n" sql)]
    (clerk/html
      [:div
       [:div {:style {:margin-bottom "5px"}}
        (clerk/code {::clerk/render-opts {:language "sql"}} sql)]
       [:div {:style {:margin-top "5px"}}
        (try
          (let [rs (xt/q xtdb sql {:current-time current-time, :key-fn :snake-case-string})
                sorted (for [rec rs] (into {} (sort-by first rec)))]
            (table sorted))
          (catch Throwable e
            [:pre (with-out-str (binding [*err* *out*] ((requiring-resolve 'clojure.repl/pst) e)))]))]])))

(defn format-inst [inst]
  (-> (inst-ms inst)
      Instant/ofEpochMilli
      (.atZone (ZoneId/of "Europe/London"))
      (.format DateTimeFormatter/ISO_LOCAL_DATE)))

(defn set-time [inst]
  (alter-var-root #'current-time (constantly inst))
  (clerk/html [:span (format "(Setting the time 🕐 to: %s)" (format-inst inst))]))

{::clerk/visibility {:code :hide, :result :show}}

^{::clerk/no-cache true} (reset)

;; Now show an update OVER historical data

;; THEN SHOW WITH SYSTEM_TIME, CAN GET ORIGINAL DATA BACK
;; SYSTEM_TIME shadows VALID_TIME, but is completely immutable
;; This is the concept of bitemporality

;; products
;; option 1 - Correcting a mistake
;; Option 2 - inserting history

;; A month later, we decide to update the price of the product.

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.tutorial-part-4))
