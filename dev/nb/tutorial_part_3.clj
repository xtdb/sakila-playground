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

;; In [part 2](tutorial_part_2), we queryied the historical timeline, to understand what changes were made.

;; In this part, we will understand how to insert historical data into XTDB.

;; How does this work with an immutable database!? Let's find out together.

;; Let's pretend the day today is 2024-01-01, and we insert a product:

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :hide}}
(set-time #inst "2024-01-01")

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 340)")

;; Let's query the day after this insert:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-02'")

;; Now, let's query against the past, in **2023**

;; We should NOT see any data, because the product was inserted into the database on `2024-01-01`:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2023-01-01'")

;; ## Inserting Historical Data

;; But let's say, we want to insert some historical data into our database, all the way back in **2022**.

;; This could an import from another system, into our XTDB golden store.

;; We achieve this in XT by setting the `xt$valid_from` and `xt$valid_to` column

^{::clerk/no-cache true}
(e "INSERT INTO product (xt$id, name, price, xt$valid_from, xt$valid_to)"
   "VALUES "
   "(1, 'Pendleton Electric Bicycle', 300, DATE '2022-01-01', DATE '2024-01-01')")

;; Now if we query in **2024**, we still get the **2024** value

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2024-01-02'")

;; But if we query in **2023**, we should see the older **2022** value:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2023-01-01'")

;; If we query in **2020**, we should see nothing:

^{::clerk/no-cache true}
(q "SELECT * FROM product FOR VALID_TIME AS OF DATE '2020-01-01'")

;; ## Conclusion

;; We've shown that it's possible to insert records into the past.
;;
;; What about if we want to update historical data? How does this work with
;; an immutable database?

;; Let's find out in [part 4](tutorial_part_4)

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.tutorial-part-3))
