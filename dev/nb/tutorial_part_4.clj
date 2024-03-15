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

;; # Changing an immutable database

;; In [part 3](tutorial_part_3), we learned how to insert historical data into XTDB.

;; In this part, we will understand how to update past data, but we will also learn how to access the raw, un-changed data also.

;; First, let's insert three versions of the same product into different points in the past:

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price, xt$valid_from)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 300, TIMESTAMP '2022-01-01 00:00:00')")

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price, xt$valid_from)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 400, TIMESTAMP '2023-01-01 00:00:00')")

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price, xt$valid_from)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 500, TIMESTAMP '2024-01-01 00:00:00')")

;; Let's prove to ourselves that querying at various points in the past, gives us the correct data:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2022-01-02'")

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2023-01-02'")

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-02'")

;; Now let's say we know that the price for `2023` was INCORRECT. This could have been due to a multitude of reasons, a developer bug, a faulty database update, or an incorrect manual data entry.

;; Let's correct the price for 2023:

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price, xt$valid_from)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 350, TIMESTAMP '2023-01-01 00:00:00')")

;; Now when we query in 2023, we get the updated price back:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2023-01-02'")

;; BUT - aren't we mutating an immutable database here?

;; Aren't we blasting over the top of original data, and thus losing that original data?

;; The answer is no. We have been updating the `VALID_TIME` line. But our XTDB database has another, completely immutable database called SYSTEM_TIME.

;; Using a query against `SYSTEM_TIME`, we can query the database exactly how it was at a point in database-time.

;; No updates to this line are possible, we can only add to the end of the timeline. We call this append-only.

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR SYSTEM_TIME AS OF DATE '2023-01-02'")

;; This is the concept of bitemporality: having two timelines.

;; One you can update:`VALID_TIME`, and one you can only ever append to: `SYSTEM_TIME`.

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.tutorial-part-4))
