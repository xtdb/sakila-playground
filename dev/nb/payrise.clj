(ns ^{:nextjournal.clerk/visibility {:code :hide}}
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

;; The distinction between 'when something happens' (`VALID_TIME`) and 'when we found out' (`SYSTEM_TIME`) can have important implications for reporting, debugging and so on.

;; Lets imagine a debugging scenario.

;; Jon received a pay rise on the 1st of Jan, but was surprised to find the wrong amount in his pay packet.
;; let us find out what his salary is.
(q "SELECT staff.username, staff.salary"
   "FROM staff"
   "WHERE staff.username = 'Jon'")

;; What was the salary on the 1st of Jan?
(q "SELECT staff.salary"
   "FROM staff FOR VALID_TIME AS OF TIMESTAMP '2024-01-01 00:00:00'"
   "WHERE staff.username = 'Jon'")

;; What about previously, on the 31st of December?
(q "SELECT staff.salary"
   "FROM staff FOR VALID_TIME AS OF TIMESTAMP '2023-12-31 00:00:00'"
   "WHERE staff.username = 'Jon'")

;; Another view is to ask for the valid time history of the salary.
(q "SELECT staff.salary, staff.xt$valid_from, staff.xt$valid_to"
   "FROM staff FOR ALL VALID_TIME"
   "WHERE staff.username = 'Jon'")

;; We can see that the salary did indeed change on the 1st, and the new salary still applies (infinity is represented as nil).

;; So why did Jon receive the incorrect pay packet? Valid time denotes the actual time of an event,
;; but this is not necessarily the same as when the event was recorded. For that we need `SYSTEM_TIME`.

(q "SELECT"
   "  staff.salary,"
   "  staff.xt$valid_from, staff.xt$valid_to,"
   "  staff.xt$system_from, staff.xt$system_to"
   "FROM staff FOR ALL SYSTEM_TIME"
   "WHERE staff.username = 'Jon'")

;; We can see Jon's salary change was recorded at `2024-02-03T09:04:14`. So in this case, the salary changed in the database
;; after the salary change should have been in effect - and after payroll.

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.payrise)
  )
