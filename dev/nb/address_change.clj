(ns ^{:nextjournal.clerk/visibility {:code :hide}}
  nb.address-change
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

;; ## A History of Address Changes

;; TODO describe hero's journey / graphic

;; In this example, the video store has been sending late fee letters to an old address. The customer had actually contacted
;; Customer Services to process an address change - but it was not carried out.

;; ---
;; ## Prelude

^{::clerk/no-cache true}
(set-time #inst "2021-03-14")

;; Jane Smith signs up for our online rental service.

^{::clerk/no-cache true}
(e "INSERT INTO address (xt$id, address, district, city_id, phone, postal_code)"
   "VALUES (1, '23 Workhaven Lane', 'Alberta', 300, '140333335568', '10672')")

^{::clerk/no-cache true}
(e "INSERT INTO customer (xt$id, email, first_name, last_name, store_id, address_id, active)"
   "VALUES (1, 'jane.smith@sakilacustomer.org', 'Jane', 'Smith', 1, 1, true)")

#_#_
^{::clerk/no-cache true}
(q "SELECT 'customer' row, customer.xt$system_from st FROM customer"
   "WHERE customer.xt$id = 1"
   "UNION"
   "SELECT 'address', address.xt$system_from FROM address"
   "WHERE address.xt$id = 1")

^{::clerk/no-cache true}
(set-time #inst "2023-05-12")

;; ---
;; ## Late Return + Mistyped Address Change

^{::clerk/no-cache true}
(set-time #inst "2023-07-22")

;; Jane recently moved house and after unpacking a rented film from the moving boxes, Jane returns it late via the post. This prompts her to then call customer services to update her address, but the agent mistypes it.

;; TODO INSERT wrong address, with comment showing the fat finger

;; ---
;; ## Late Fees Letter

^{::clerk/no-cache true}
(set-time #inst "2023-09-10")

;; TODO Letterhead "overdue" image

;; The company sends automated letters to the invalid address regarding non-payment of late fees.

;; ---
;; ## Debt Collectors Finds Jane's New Address

^{::clerk/no-cache true}
(set-time #inst "2023-12-18")

;; Eventually, the debt department is engaged to chase down Jane for payment.
;; They find that Jane now appears to live at a new address along with the adress when she moved in (2023-07-17).
;; Seeing that the current address in the system is probably wrong, they record this new information and ensure that the timeline of Jane's address move is recorded clearly.
;; the customer is found - but is highly annoyed that the address change was not processed. She remembers phoning, and even has kept
;; a reference number from the phone call.

;; ---

^{::clerk/no-cache true}
(set-time #inst "2023-12-19")

;; The customer calls CS, they change the address retroactively.
;; Notice how the valid time is included in the `INSERT` and `UPDATE` statement.

^{::clerk/no-cache true}
(e "INSERT INTO address (xt$id, address, district, city_id, phone, postal_code, xt$valid_from)"
   "VALUES (2, '1121 Loja Avenue', 'California', 449, '838635286649', '17886', TIMESTAMP '2023-05-12 00:00:00')")

^{::clerk/no-cache true}
(e "UPDATE customer"
   "FOR PORTION OF VALID_TIME FROM TIMESTAMP '2023-05-12 00:00:00' TO NULL"
   "SET address_id = 2"
   "WHERE customer.xt$id = 1")

;; ---

;; Importantly, historical views such as 'addresses held by' are corrected.

^{::clerk/no-cache true}
(q "SELECT c.first_name, c.last_name, a.address, a.xt$valid_from change_date"
   "FROM address FOR ALL VALID_TIME a"
   "JOIN customer FOR ALL VALID_TIME c ON c.address_id = a.xt$id"
   "WHERE c.xt$id = 1")

;; ---

^{::clerk/no-cache true}
(set-time #inst "2024-01-17")

;; Problem, in dispute of the late fee Jane has gone to an ombudsman for assistance. The ombudsman has asked for evidence regardling
;; the address history - when it was edited and what edits were made.

^{::clerk/no-cache true}
(q "SELECT"
   "  c.address_id,"
   "  c.xt$valid_from valid_from, c.xt$valid_to valid_to,"
   "  c.xt$system_from system_from, c.xt$system_to system_to"
   "FROM customer FOR ALL VALID_TIME FOR ALL SYSTEM_TIME c"
   "WHERE c.xt$id = 1")

;; The presentation of the evidence satisfied the ombudsman that the companies record keeping was satisfactory in order to
;; resolve disputes.

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.address-change)
  )
