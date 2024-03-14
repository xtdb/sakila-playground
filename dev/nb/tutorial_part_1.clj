(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.tutorial-part-1
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
  (clerk/html [:span (format "(Setting the time 🕐 to: %s)" (format-inst inst))]))

{::clerk/visibility {:code :hide, :result :show}}

^{::clerk/no-cache true} (reset)

;; # Avoiding a lossy database

;; Imagine a system that stores product data.

;; Suppose somebody decides to delete a product from our database.

;; ```
;; DELETE product WHERE product.id = 1;
;; ```

;; In a traditional database, this record is now gone for good, and
;; unrecoverable (except for restoring from backups, which is
;; expensive, time-consuming and notoriously unreliable!).

;; One common workaround is the use of a status column:

;; ```
;; UPDATE product SET status = 'UNAVAILABLE'
;; WHERE product.id = 1;
;; ```

;; The downside of this approach is that *all* queries to the product
;; table now need to be aware of this workaround and add explicit
;; clauses to their queries.

;; ```
;; SELECT * FROM product WHERE status <> 'UNAVAILABLE'
;; ```

;; Another downside is that we no longer have any historic record of
;; when a status changed.

;; *This is a trivial example but one that clearly demonstrates the
;; fragility and complexity of managing time in data systems.*

;; ## Controlling the timeline with XTDB

;; Using an immutable database we keep everything, including the
;; history of any change to the database. Therefore, we can get back
;; deleted data.

;; For example, let's set up a scenario by inserting some product records:

;; Let's pretend the day we are inserting these records is 2024-01-01.

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :show}}
(set-time #inst "2024-01-01")

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle'),"
   "(2, 'Bicycle Pump')")

;; Let's query these products:

^{::clerk/no-cache true}
(q "SELECT * FROM product")

;; A month later, someone deletes the product.

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :show}}
(set-time #inst "2024-02-01")

^{::clerk/no-cache true}
(e "DELETE FROM product WHERE product.name = 'Pendleton Electric Bicycle'")

;; Let's check that the bicycle is no longer in our database:

^{::clerk/no-cache true}
(q "SELECT * FROM product")

;; The product is gone!

;; However, don't worry. Since the database is immutable, we can make
;; a historical query different time. We can do this by adding a
;; qualifier to the query:

^{::clerk/no-cache true}
(q "SELECT *"
   "FROM product"
   "FOR VALID_TIME AS OF DATE '2024-01-01'")

;; ## Conclusion

;; We've shown that it's possible to use standard SQL to make
;; historical queries against an immutable database, to bring back
;; deleted data.

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.tutorial-part-1)
  )
