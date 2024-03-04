(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.address-change2
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk-slideshow :as slideshow]
            [xtdb.api :as xt]
            [xtdb.node :as xt-node])
  (:import (java.time Instant ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)))

{::clerk/visibility {:code :hide, :result :hide}}

(clerk/add-viewers!
  [slideshow/viewer
   {:pred #(instance? ZonedDateTime %)
    :transform-fn (clerk/update-val #(.format % DateTimeFormatter/ISO_LOCAL_DATE_TIME))}])

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
    (clerk/code {::clerk/render-opts {:language "sql"}} sql)))

(defn table [rs]
  (if (seq rs)
    (clerk/table rs)
    (clerk/table [{"" "no results"}])))

(defn q [& sql]
  (let [sql (str/join "\n" sql)]
    (clerk/html
      [:div
       [:div
        (clerk/code {::clerk/render-opts {:language "sql"}} sql)]
       [:div
        (try
          (let [rs (xt/q xtdb sql {:current-time current-time, :key-fn :snake-case-string})]
            (table rs))
          (catch Throwable e
            [:pre (with-out-str (binding [*err* *out*] ((requiring-resolve 'clojure.repl/pst) e)))]))]])))

(defn format-inst [inst]
  (-> (inst-ms inst)
      Instant/ofEpochMilli
      (.atZone (ZoneId/of "Europe/London"))
      (.format DateTimeFormatter/ISO_LOCAL_DATE)))

(defn set-time [inst]
  (alter-var-root #'current-time (constantly inst))
  (clerk/html [:span (format "🕐: %s" (format-inst inst))]))

{::clerk/visibility {:code :hide, :result :show}}

^{::clerk/no-cache true} (reset)

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :hide}}
(set-time #inst "2021-09-10")

;; ## Understanding your customer

;; How do you record information about the history of your customers in a structured, auditable a way?

;; Let's examine a trivial requirement to correctly record and interpret the address changes of a customer...

;; ---
;; ## A mutable database schema

;; The most basic database schema for recording customer information might be a customers table that simply includes a freeform address column:

^{::clerk/no-cache true}
(e "INSERT INTO customer0 (xt$id, name, address)"
   "VALUES (1, 'Jane Smith', '23 Workhaven Lane, Alberta, 10672')")

;; Finding out the latest address for a customer is simple:

^{::clerk/no-cache true}
(q "SELECT c.address"
   "FROM customer0 AS c"
   "WHERE c.xt$id = 1")

;; In this model however, we have no means of capturing additional addresses or structuring those addresses...

;; ---
;; ## A mutable database schema #2

;; A more realistic schema would use an independent address table:

^{::clerk/no-cache true}
(e "INSERT INTO address1 (xt$id, address, district, postal_code)"
   "VALUES (1, '23 Workhaven Lane', 'Alberta', '10672')")

^{::clerk/no-cache true}
(e "INSERT INTO customer1 (xt$id, name, address_id)"
   "VALUES (1, 'Jane Smith', 1)")

;; But what should we do here if we were to find out our customer changed address *last month*? Should we simply ignore that information and accept the risk the we some day might regret having not recorded it? Should we write it in a free-for-all 'customer_notes' column?

;; Recording historical information accurately is essential for: compliance reporting, troubleshooting, and forecasting. If you only record the information about the *current* state of the world by default, then coping with new time-related requirements gets tricky.

;; ---
;; ## Curating the timeline of your business data

;; In other SQL databases, recording backfilled or corrected data is a challenge that requires careful schema and query design. In contrast, XTDB models the validity of data being recorded across every table, without having to make any explicit decisions or changes to the schema. This universal mechanism is helpful for satisfying all manner of backfilling, correction and scheduling requirements, whilst maintaining full auditability.

;; Every row maintains its very own timeline that you can use to capture important information. For example, we can specify a time that records _when_ newly learned customer information was best known to be true, but without exposing that complexity to the normal view of our application schema and queries:

^{::clerk/no-cache true}
(e "INSERT INTO address2 (xt$id, xt$valid_from, address, district, postal_code)"
   "VALUES (1, DATE '2021-03-14', '236 Bude Close', 'Washington', '41491'),"
   "       (2, DATE '2021-09-10', '12 Upper Street', 'Washington', '88493')")

^{::clerk/no-cache true}
(e "INSERT INTO customer2 (xt$id, xt$valid_from, name, address_id)"
   "VALUES (1, DATE '2021-03-14', 'Jane Smith', 1),"
   "       (1, DATE '2021-09-10', 'Jane Smith', 2)")

^{::clerk/no-cache true}
(q "SELECT c.name, a.address, a.xt$valid_from"
   "FROM address2 AS a"
   "JOIN customer2 AS c ON c.address_id = a.xt$id"
   "WHERE c.xt$id = 1")

;; Here we see that the earlier address is not returned by default, however it is still readily accessible whenever we may need it in the future:

^{::clerk/no-cache true}
(q "SELECT c.name, a.address, a.xt$valid_from"
   "FROM address2 FOR ALL VALID_TIME AS a"
   "JOIN customer2 FOR ALL VALID_TIME AS c ON c.address_id = a.xt$id"
   "WHERE c.xt$id = 1")

;; ---
;; ## Correcting and auditing your business data

;; In the normal course of recording any kind of historical information, it is often necessary to be able to correct data and compensate for mistakes. Therefore, in addition to capturing the validity of records, XTDB maintains a full audit history that is capable of revealing changes to history, before and after any corrections have been applied.

;; For example, if we later find out that the previous street address of `12 Upper Street` was meant to be `11` we can make a correcting INSERT that affects the same validity period (overwriting the regular view of the previous insert):

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :hide}}
(set-time #inst "2022-01-03")

^{::clerk/no-cache true}
(e "INSERT INTO address2 (xt$id, xt$valid_from, address, district, postal_code)"
   "VALUES (2, DATE '2021-09-10', '11 Upper Street', 'Washington', '88493')")

^{::clerk/no-cache true}
(q "SELECT c.name, a.address, a.xt$valid_from"
   "FROM address2 FOR ALL VALID_TIME AS a"
   "JOIN customer2 FOR ALL VALID_TIME AS c ON c.address_id = a.xt$id"
   "WHERE c.xt$id = 1")

;; Viewing the audit history of versions prior to corrections is trivial:

^{::clerk/no-cache true}
(q "SELECT c.name, a.address, a.xt$valid_from, a.xt$system_from"
   "FROM address2 FOR ALL VALID_TIME FOR ALL SYSTEM_TIME AS a"
   "JOIN customer2 FOR ALL VALID_TIME FOR ALL SYSTEM_TIME AS c ON c.address_id = a.xt$id"
   "WHERE c.xt$id = 1")

;; Such curated views of history - across the full evolution of your database - can be essential both for satisfying external audits by regulators, but also for enabling comprehensive analysis and decision support for your business without introducing additional sources of error-prone complexity into your systems (extensive schema workarounds, ETL etc.)

;; TODO Controlled Forgetting vs Controlled Remebering

;; just about assigning relations different values in valid time
;; it's about mechanical state changes, not about modelling directly, just membership -- "i want this row to change" -- if modelling requirements line up then great
;; valid time is not useful for fuzzy / low-granularity / possible speculative projections / probablistic RA

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.address-change2)
  )
