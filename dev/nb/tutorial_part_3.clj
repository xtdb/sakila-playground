(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.tutorial-part-3
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

;; # Updating the Past

;; In XTDB, we allow uses to make updates into the past.

;; How does this work with an immutable database?! Let's find out together.

;; Let's pretend the first version of a product is inserted on 2024-01-01.

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :show}}
(set-time #inst "2024-01-01")

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 340)")

;; Let's query the day after this insert:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-02'")

;; Now, let's query against the past - 2023
;; We should NOT see any data, because the product was inserted into the database on 2024-01-01:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2023-01-01'")

;; But let's say, we want to insert some historical data into our database - 2022.
;; This could an import from another system, into our golden store
;; We can do this in XT but setting the xt$valid_from column

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price, xt$valid_from)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 300, TIMESTAMP '2022-01-01 00:00:00')")

;; Now if we query in 2024, we still get the 2024 value

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-02'")

;; But if we query in 2023, we should see the older 2022 value:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2023-01-01'")

;; If we query in 2021, we should see nothing:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2020-01-01'")


;; NEED TO INSERT WITH VALID_TIME

;; THEN SHOW WITH SYSTEM_TIME, CAN GET ORIGINAL DATA BACK
;; SYSTEM_TIME shadows VALID_TIME, but is completely immutable
;; This is the concept of bitemporality

;; A month later, we decide to update the price of the product.

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :show}}
(set-time #inst "2024-02-01")

^{::clerk/no-cache true}
(e "UPDATE product"
   "SET price = 360"
   "WHERE product.name = 'Pendleton Electric Bicycle'")

;; Let's check the new price:

^{::clerk/no-cache true}
(q "SELECT * FROM product")

;; A month later, with part costs still increasing, we increase the price again.

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :show}}
(set-time #inst "2024-03-01")

^{::clerk/no-cache true}
(e "UPDATE product"
   "SET price = 400"
   "WHERE product.name = 'Pendleton Electric Bicycle'")

^{::clerk/no-cache true}
(q "SELECT * FROM product")

;; Our CFO is running a report about January sales and needs to know
;; the price of every product as of 2024-01-15.

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-15'")

;; Now the CFO wants to how the prices have increased over Q1.

^{::clerk/no-cache true}
(q "SELECT product.*, product.xt$valid_from, product.xt$valid_to"
   "FROM product"
   "FOR ALL VALID_TIME"
   "ORDER BY product.xt$valid_from")

;; When did the bicycle price first exceed $350 ?

^{::clerk/no-cache true}
(q "SELECT product.*, product.xt$valid_from, product.xt$valid_to"
   "FROM product"
   "FOR ALL VALID_TIME"
   "WHERE product.price > 350"
   "ORDER BY product.xt$valid_from"
   "LIMIT 1")

;; Yes, it was the price change of 2024-02-01 where the bicycle's
;; price exceeded $350.

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.tutorial-part-3))
