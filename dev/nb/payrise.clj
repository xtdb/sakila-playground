(ns ^{::clerk/visibility {:code :hide}}
  nb.payrise
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [xtdb.demo.db :as db])
  (:import (java.time ZonedDateTime)))

{::clerk/visibility {:code :hide, :result :hide}}

(clerk/add-viewers!
  [{:pred #(instance? ZonedDateTime %)
    :transform-fn (clerk/update-val #(.format % java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME))}])

(defn table [rs]
  (when (seq rs)
    (clerk/table rs)))

(defn q [& sql]
  (let [sql (str/join "\n" sql)]
    (clerk/col
      (clerk/code {::clerk/render-opts {:language "sql"}} sql)
      (let [rs (db/q sql {:key-fn :snake-case-string})]
        (table rs)))))

{::clerk/visibility {:code :hide, :result :show}}

;; ## Pay rise example

;; in this notebook we demonstrate valid times ability to include information we learn
;; about the past, without erasing what-we-knew at some previous time.
(q "SELECT staff.username, staff.salary"
   "FROM staff"
   "WHERE staff.username = 'Jon'")

;; Jon's salary is currently `2900`. Let us find out what it was in the past.
;; first we will query our staff table across `SYSTEM_TIME` to find
;; changes to Jon's staff record.
(q "SELECT staff.xt$system_from, staff.salary"
   "FROM staff FOR ALL SYSTEM_TIME"
   "WHERE staff.username = 'Jon'")

;; We can see jons salary changes at 2024-02-03. Or did it?
;; Remember `SYSTEM_TIME` denotes the event time of the record change.
;; let us check `VALID_TIME`.
(q "SELECT staff.xt$system_from, staff.xt$valid_from, staff.salary"
   "FROM staff FOR ALL VALID_TIME"
   "WHERE staff.username = 'Jon'")

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.payrise)
  )
