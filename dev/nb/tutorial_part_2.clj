(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.tutorial-part-2
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
  (clerk/html [:span (format "(Setting the time of the demo 🕐 to: %s)" (format-inst inst))]))

{::clerk/visibility {:code :hide, :result :show}}

^{::clerk/no-cache true} (reset)

;; # Understanding change

;; In [part 1](tutorial_part_1), we covered deleted data, and the fact that in an immutable database, data is never truly gone.

;; In this part, we'll expand more
;; on the idea of querying the timeline.

;; Let's use a single record for this example.

;; Let's pretend the first version is inserted on 2024-01-01.

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :show}}
(set-time #inst "2024-01-01")

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 340)")

;; (Notice how we don't have to create a database table explicitly, or tell our database about the columns - the database will learn the schema from the data we give it)

;; Let's query this product:

^{::clerk/no-cache true}
(q "SELECT * FROM product")

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

;; Let's say we need to do an audit query, and we need to know
;; the price of every product as of 2024-01-15.

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-15'")

;; Here you can see we have the correct historical price for 2024-01-15, which is 340

;; Now lets say our the CFO wants to how the prices have increased over Q1?

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
  (clerk/show! 'nb.tutorial-part-2)
  )
