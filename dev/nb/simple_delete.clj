(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.simple-delete
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk-slideshow :as slideshow]
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
  (clerk/html [:span (format "🕐: %s" (format-inst inst))]))

{::clerk/visibility {:code :hide, :result :show}}

^{::clerk/no-cache true} (reset)

^{::clerk/no-cache true ::clerk/visibility {:code :hide :result :hide}}
(set-time #inst "2021-09-10")

;; ## Inserting records

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

;; Now delete
(set-time #inst "2021-09-20")

^{::clerk/no-cache true}
(e "DELETE FROM customer0")

^{::clerk/no-cache true}
(q "SELECT c.address"
   "FROM customer0 AS c"
   "WHERE c.xt$id = 1")

;; Now time-travel
(set-time #inst "2021-09-10")

^{::clerk/no-cache true}
(q "SELECT c.address"
   "FROM customer0 AS c"
   "WHERE c.xt$id = 1")

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.simple-delete)
  )
