(ns xtdb.demo.history
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [xtdb.api :as xt])
  (:refer-clojure :exclude [rand rand-int rand-nth shuffle])
  (:import (java.io PushbackReader)
           (java.time Duration Instant)
           (java.util Collections Random)))

(def ^:dynamic ^Random *rng* (Random. 42))
(defn rand [] (.nextDouble *rng*))
(defn rand-int [n] (.nextInt *rng* n))
(defn rand-nth [coll] (if (indexed? coll) (nth coll (rand-int (count coll))) (recur (vec coll))))
(defn rand-nth-or-nil [coll] (when (seq coll) (rand-nth coll)))
(defn shuffle [coll] (Collections/shuffle coll *rng*))

(defn foreign-key? [col-kw]
  (str/ends-with? (name col-kw) "_id"))

;; find all the :fk val pairs
(defn find-foreign-deps [record]
  (for [[k v] record
        :when (foreign-key? k)]
    [k v]))

(defn foreign-table [fk]
  (assert (foreign-key? fk))
  (case fk
    :manager_staff_id :staff
    (let [parts (str/split (name fk) #"\_")
          table-name (str/join "_" (butlast parts))]
      (keyword table-name))))

(comment

  (foreign-table :actor_id)
  (foreign-table :payment_id)

  )

(defn sql-munge [kw]
  (case kw
    :xt/id "xt$id"
    (name kw)))

(defn insert-op [table records]
  (let [cols (-> (into #{} (mapcat keys records)))
        table-name (sql-munge table)
        col-list (str/join "," (map sql-munge cols))
        value-placeholders (str/join "," (repeat (count cols) "?"))]

    {:xt-dml [(into [:put-docs table] records)]
     :sql (format "INSERT INTO %s (%s) VALUES (%s)" table-name col-list value-placeholders)
     :args [(mapv (fn [record] (mapv record cols)) records)]}))

;; todo film_actor, join-deps

(defn satisfy-foreign-deps
  ([history foreign-deps] (satisfy-foreign-deps history foreign-deps #{}))
  ([history foreign-deps cycle-break]
   (reduce
     (fn [[history operations] [fk id]]
       (let [table (foreign-table fk)
             {:keys [state, sample-state]} (history table)]
         (if (or (contains? state id) (cycle-break [table id]))
           [history operations]
           (let [self-fdeps (find-foreign-deps (sample-state id))
                 [history foreign-operations] (satisfy-foreign-deps history self-fdeps (conj cycle-break [table id]))]
             [(-> history
                  (update-in [table :remaining-ids] disj id)
                  (assoc-in [table :state id] (sample-state id)))
              (into operations cat [foreign-operations [(insert-op table [(sample-state id)])]])]))))
     [history []]
     foreign-deps)))

(defn add-records [history table new-records]
  (let [foreign-deps (mapcat find-foreign-deps new-records)
        [history ops] (satisfy-foreign-deps history foreign-deps)
        update-table
        (fn [{:keys [remaining-ids, state] :as table-model} record]
          (let [{:xt/keys [id]} record]
            (-> table-model
                (assoc :remaining-ids (disj remaining-ids id)
                       :state (assoc state id record)
                       :next-id (max (:next-id table-model 0) (inc id))))))
        history (update history table #(reduce update-table % new-records))]
    [history (conj ops (insert-op table new-records))]))

(defn tx-add-stock [history _time]
  ;; find inventory for some film-ids, satisfy its films (and dependents like actors, actor films, categories, category films)
  (let [n-records (inc (rand-int 16))
        {:keys [inventory]} history
        {:keys [remaining-ids, sample-state]} inventory
        new-records (map sample-state (take n-records remaining-ids))
        history (reduce (fn [history record] (update history :available-inventory (fnil conj #{}) (:xt/id record))) history new-records)]
    (add-records history :inventory new-records)))

(defn tx-add-customer [history _time]
  (let [{:keys [customer]} history
        {:keys [remaining-ids, sample-state]} customer
        new-records (map sample-state (take 1 remaining-ids))]
    (add-records history :customer new-records)))

(defn- mark-rental [history rental]
  (let [{rental-id :xt/id, :keys [inventory-id]} rental]
    (-> history
        (update :current-rentals (fnil conj #{}) rental-id)
        (update :available-inventory disj inventory-id))))

(defn- mark-return [history rental]
  (let [{rental-id :xt/id, :keys [inventory-id]} rental]
    (-> history
        (update :current-rentals disj rental-id)
        (update :available-inventory conj inventory-id))))

(defn tx-add-rental [history _time]
  (let [{:keys [rental]} history
        {:keys [remaining-ids, sample-state]} rental
        new-records (map sample-state (take 1 remaining-ids))
        history (reduce (fn [history record]
                          (if (nil? (:return_date record))
                            (mark-rental history rental)
                            history))
                        history
                        new-records)
        rental-id-set (set (map :xt/id new-records))
        payment-records (->> history :payment :sample-state vals (filter (comp rental-id-set :rental_id)))
        [history operations] (add-records history :rental new-records)
        [history payment-operations] (add-records history :payment payment-records)]
    [history (into operations payment-operations)]))

(defn tx-start-rental [history time]
  (let [{:keys [available-inventory, rental, customer]} history
        inventory-id (rand-nth-or-nil available-inventory)
        customer-id (some-> (keys (:state customer)) not-empty rand-nth)
        rental-id (:next-id rental)
        record {:xt/id rental-id
                :inventory_id inventory-id,
                :customer_id customer-id,
                :staff_id 1,
                :rental_date time
                :return_date nil}]
    (if (and inventory-id customer-id)
      (let [history (mark-rental history record)]
        (add-records history :rental [record]))
      [history []])))

(defn tx-end-rental [history time]
  (let [{:keys [current-rentals, payment, inventory, rental, film]} history
        rental-id (rand-nth-or-nil current-rentals)]
    (if-not rental-id
      [history []]
      (let [rental-record (-> rental :state (get rental-id))
            inventory-id (:inventory_id rental-record)
            inventory-record (-> inventory :state (get inventory-id))
            film-id (:film_id inventory-record)
            new-rental-record (assoc rental-record :return_date time)
            film-record (-> film :state (get film-id))
            payment-record
            {:xt/id (:next-id payment),
             :amount (:rental_rate film-record)
             :customer_id (:customer_id rental-record)
             :payment_date time
             :rental_id (:xt/id rental-record)
             :staff_id (:staff_id rental-record)}
            history (mark-return history rental-record)
            [history operations] (add-records history :rental [new-rental-record])
            [history payment-operations] (add-records history :payment [payment-record])]
        [history (into operations payment-operations)]))))

(defn tx-change-price [history _time]
  (let [{:keys [film]} history
        film (rand-nth-or-nil (vals (:state film)))]
    (if film
      (let [new-film (update film :rental_rate + (rand-nth [0.25, 0.5, 1.0, 1.5, 2.0]))]
        (add-records history :film [new-film]))
      [history []])))

(defn satisfy-init-deps [history]
  (reduce-kv
    (fn [history _table {:keys [state]}]
      (reduce-kv
        (fn [history _id record]
          (let [foreign-deps (find-foreign-deps record)
                [history] (satisfy-foreign-deps history foreign-deps)]
            history))
        history
        state))
    history
    history))

(defn fix-record [table-name record]
  (case table-name
    ;; absent handling is wierd at time of writing
    "rental" (if (:return_date record) record (assoc record :return_date nil))
    record))

(defn init-history []
  (->> (for [table-name ["actor"
                         "address"
                         "category"
                         "city"
                         "country"
                         "customer"
                         "film"
                         "film_actor"
                         "film_category"
                         "inventory"
                         "language"
                         "payment"
                         "rental"
                         "staff"
                         "store"]
             :let [edn-file (io/resource (format "sakila/%s.edn" table-name))
                   file-state (atom {})
                   _ (with-open [rdr (io/reader edn-file)
                                 pb-rdr (PushbackReader. rdr)]
                       (let [read-next (fn [] (edn/read {:eof nil, :readers {'time/instant #(Instant/parse %)}} pb-rdr))]
                         (loop []
                           (when-some [obj (read-next)]
                             (swap! file-state assoc (:xt/id obj) (fix-record table-name obj))
                             (recur)))))]]
         (case table-name
           ;; reference tables, do not see transactions (for now)
           ("city" "country" "language" "staff" "store" "category" "actor" "film_actor" "film_category")
           [(keyword table-name)
            {:init-state @file-state
             :sample-state @file-state,
             :state @file-state,
             :remaining-ids #{}}]
           ;; other tables start empty and accrete over time
           [(keyword table-name)
            {:init-state {}
             :sample-state @file-state,
             :remaining-ids (set (keys @file-state))
             :state {}}]))
       (into {})
       (satisfy-init-deps)))

(def start-time (Instant/parse "2023-01-01T00:00:00Z"))

(def end-time (Instant/parse "2024-02-05T00:00:00Z"))

(def tx-time
  "The amount of time between transactions"
  (Duration/parse "PT1H"))

(def retry-count
  "The number of times the generator can try again if it is unable to generate a transaction for the given time."
  10)

(def transactions-weighted
  ;; poor mans weighted sampler, I have code for an alias sampler but would need to bring in a minmaxpriorityqueue dep
  ;; should not matter here
  (->> {#'tx-add-stock 2
        #'tx-change-price 1
        #'tx-add-customer 4
        #'tx-add-rental 16
        #'tx-start-rental 16
        #'tx-end-rental 16}
       (mapcat (fn [[f weight]] (repeat weight f)))
       vec))

(defn generate-transactions [history]
  (loop [time (.plus start-time tx-time)
         h history
         transactions []
         retry-counter retry-count]
    (if (<= (compare end-time time) 0)
      transactions
      (let [gen-transaction (rand-nth transactions-weighted)
            current-time (.plus time tx-time)
            [h operations] (gen-transaction h current-time)]
        (cond
          (seq operations)
          (recur current-time
                 h
                 (conj transactions {:opts {:system-time time}
                                     :tx-ops (vec (mapcat :xt-dml operations))})
                 retry-count)

          (= 0 retry-count)
          (recur current-time h transactions retry-count)

          :else
          (recur time h transactions (dec retry-counter)))))))

(defn setup-node [node seed]
  (binding [*rng* (Random. seed)]
    (let [initial-history (init-history)
          transactions (generate-transactions initial-history)]
      (doseq [[table {:keys [init-state]}] initial-history
              :when init-state]
        (->> init-state
             (sort-by key)
             (map val)
             (partition-all 512)
             (run! (fn [puts] (xt/submit-tx node [(into [:put-docs table] puts)] {:system-time start-time})))))
      (run! #(xt/submit-tx node (:tx-ops %) (:opts %)) transactions))))

(comment
  (def h (init-history))
  (keys h)
  (update-vals h (comp count :remaining-ids))
  (second (tx-add-customer h))
  )

;; films are added over time
;; - add one film
;;   - add some initial inventory
;;   - further inventory may be added
;; - add n films
;; the rental duration and rate can vary
;; - increase price
;; data entry mistakes are possible
;; - flub a price or duration or name
;; - later correct

;; inventory
;; added (new stock)
;; mark lost (tape lost, do these columns exist)?

;; transactions
;; new stock
;; add the films, categories, actors (if they do not exist) and inventory
;; sometimes include a name mistake, or price mis entry

;; price increase
;; increase the rental rates of a few films

;; mistaken name correction
;; fix a mistaken name

;; address change

;; new customer

;; customer rents a film
;; customer returns a film (pays)
;; customer returns a film late (pays + late fee)

;; offline period
;; future time scheduled updates?
