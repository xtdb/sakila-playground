(ns ^{::clerk/visibility {:code :hide}}
 nb.tx-history
  (:require [nextjournal.clerk :as clerk]
            [xtdb.api :as xt]
            [xtdb.node :as xt-node])
  (:import (java.time Instant ZoneId ZonedDateTime LocalDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Date)))

;; ## Transaction histories visualisation

;; ### System time

;; The system time of the database reflects the characteristics of physical time 
;; in the context of classical physics: 
;;it is supposed to be irreversible, it is supposed to flow uniformly, and it is supposed to be universal. 
;; In the realm of databases, this means that if we consider any two different transactions, 
;; we can definitively say whether one transaction occurs before the other 
;; or if they both happen at the same time.

;; In real-life database implementations, it is challenging to meet these requirements exactly,
;; but they can be achieved for most practical purposes. 
;; System time allows us to place every transaction on a time scale, 
;; and hence it enables us to replay transactions and restore 
;; the database state for any given system time value.

;; ### Simple example

;; Consider a simplified customer data model that comprises an `id`, `address`, and communication `preference`. 
;; The `address` is represented as an integer, 
;; while the communication `preference` is categorized as either `undefined`, `phone`, `letter`, or `email`. 
;; Additionally, we aim to generate a transaction log that captures customer activities 
;; and stores this information in a database. 
;; With each customer action, we update the customer's information 
;; and simultaneously insert a corresponding entry into an `event` table. 
;; This `event` table contains only an event `id` and a field named `updated_fields`, 
;; which specifies which customer data fields were modified.

{::clerk/visibility {:code :hide :result :hide}}

(defn format-date 
  [date-time]
  (.format date-time DateTimeFormatter/ISO_LOCAL_DATE_TIME))

(clerk/add-viewers!
 [{:pred #(instance? ZonedDateTime %)
   :transform-fn (clerk/update-val #(format-date %))}])

(defonce xtdb nil)

(defn reset []
  (alter-var-root
   #'xtdb
   (fn [n]
     (when n (.close n))
     (xt-node/start-node {}))))

^{::clerk/no-cache true} (reset)

(defn rand-inst-range
  [^Date start ^Date end size]
  (let [start-ms (.getTime start)
        end-ms (.getTime end)]
    (mapv #(java.util.Date. %)
          (-> (java.util.Random.)
              (.longs size start-ms end-ms)
              .sorted
              .toArray))))

^{::clerk/no-cache true
  ::clerk/visibility {:result :hide}}
(def tx-log-size 100)

^{::clerk/no-cache true}
(def tx-log
  (->> (rand-inst-range #inst "2020" #inst "2024" tx-log-size)
       (mapv #(hash-map :st %
                        :xt/id (str (rand-int 25))
                        :updated-fields (rand-nth [[:address] [:preference] [:address :preference]])))
       (reduce (fn [state {:keys [updated-fields] :xt/keys [id] :as event}]
                 (let [{:keys [_address preference] :as recent-state} (last (get state id [{}]))
                       state-udpate (cond-> event
                                      (:address (set updated-fields)) #_update-address (assoc :address (rand-int 100000))
                                      (:preference (set updated-fields)) #_update-preference (assoc :preference (rand-nth (vec (disj #{"email" "phone" "letter"} preference)))))]
                   (update state id (fnil conj [])
                           (merge recent-state state-udpate)))) {})
       vals
       (apply concat)
       (sort-by :st)))

^{::clerk/no-cache true
  ::clerk/visibility {:result :hide}}
(doseq [{:keys [st updated-fields] :xt/keys [id] :as tx} tx-log]
  (xt/submit-tx xtdb 
                [[:put-docs :customer (dissoc tx :st :updated-fields)]
                 [:put-docs :event {:xt/id (str "customer_" id) :updated-fields updated-fields}]] 
                {:system-time st}))

;; Let's see what the actual state of the customers table with the simple query

^{::clerk/no-cache true
 ::clerk/viewer clerk/code
 ::clerk/visibility {:result :show}
 ::clerk/render-opts {:language "sql"}}
(def customers-actual-query
  "SELECT CAST(customer.xt$id AS INTEGER) AS id,
   customer.address,
   customer.preference
FROM customer  
ORDER BY 
CAST (customer.xt$id AS INTEGER)")

^{::clerk/no-cache true
  ::clerk/visibility {:result :hide}}
(def customers-current-state (xt/q xtdb customers-actual-query {}))


^{::clerk/no-cache true
  ::clerk/visibility {:result :show}}
(clerk/table customers-current-state)

;; Let's now take a look at the evolution of customer table with a bit more complex query.
;; Now we querying customer table for all system times value and also we'll attaching the metadata from event's table to result set.


^{::clerk/no-cache true
  ::clerk/viewer clerk/code
  ::clerk/visibility {:result :show}
  ::clerk/render-opts {:language "sql"}}
(def customers-history-query
  "SELECT CAST(customer.xt$id AS INTEGER) AS id,
   customer.address,
   customer.preference,  
   customer.xt$system_from AS system_from, 
   customer.xt$system_to AS system_to, 
   event.updated_fields as update_type
FROM customer 
FOR ALL SYSTEM_TIME
JOIN event FOR ALL system_time 
     ON 'customer_'||customer.xt$id=event.xt$id 
     AND customer.xt$system_from=event.xt$system_from 
ORDER BY 
CAST (customer.xt$id AS INTEGER), 
customer.xt$system_from")

^{::clerk/no-cache true
  ::clerk/visibility {:result :hide}}
(def customers-history (xt/q xtdb customers-history-query {}))


^{::clerk/no-cache true
  ::clerk/visibility {:result :show}}
(clerk/table customers-history)

;; We can see now evolution of state for every customer in the table. 
;; Simple visualisation of this evolution shown on the figure below.

^{::clerk/no-cache true
  ::clerk/visibility {:result :show}}
(clerk/vl {:nextjournal.clerk/width :wide} 
 {:data {:values (mapv #(update % :system-from format-date) customers-history)}
  :transform [{:calculate "join(datum['update-type'], ' and ')"
               :as "Update reason"}
              #_{:calculate "toNumber(datum.id)"
               :as "customer id"}] 
  :vconcat [{:width 600
             :title "Customers transaction history"
             :encoding {:x {:field "system-from"
                            :type "temporal"
                            :title "system time"
                            :scale {:domain {:param "brush"}}}
                        :y {:field "id" #_"customer id"
                            :type  "ordinal"}}
             :layer [{:description "tx-timeline"
                      :mark {:type "line"
                             :point false}
                      :encoding {:detail {:field "id"}}}
                     {:description "Timeline outline"
                      :mark {:type "point"
                             :filled false
                             :size 125}}
                     {:description "Timeline points"
                      :mark {:type "point"
                             :filled true
                             :opacity 1.0
                             :size 75}
                      :encoding {:color {:field "Update reason"
                                         :type "nominal"}
                                 :tooltip [{:field "id"}
                                           {:field "address"}
                                           {:field "preference"}]}}]}
            {:width 600
             :title "System time"
             :mark "tick"
             :params [{:name "brush" 
                       :select {:type "interval"
                                :encodings ["x"]}}]
             :encoding {:x {:field "system-from" :type "temporal"}}}]})

