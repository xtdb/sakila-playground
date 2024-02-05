(ns xtdb.demo.history
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [xtdb.api :as xt])
  (:import (java.io PushbackReader)
           (java.time Duration Instant)))

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

    {:xt-dml (mapv #(xt/put table %) records)
     :sql (format "INSERT INTO %s (%s) VALUES (%s)" table-name col-list value-placeholders)
     :args [(mapv (fn [record] (mapv record cols)) records)]}))

;; todo film_actor, join-deps

(defn satisfy-foreign-deps
  ([history foreign-deps] (satisfy-foreign-deps history foreign-deps #{}))
  ([history foreign-deps cycle-break]
   (reduce
     (fn [[history operations] [fk id]]
       (let [table (foreign-table fk)
             {:keys [state, init-state]} (history table)]
         (if (or (contains? state id) (cycle-break [table id]))
           [history operations]
           (let [self-fdeps (find-foreign-deps (init-state id))
                 [history foreign-operations] (satisfy-foreign-deps history self-fdeps (conj cycle-break [table id]))]
             [(-> history
                  (update-in [table :remaining-ids] disj id)
                  (assoc-in [table :state id] (init-state id)))
              (into operations cat [foreign-operations [(insert-op table [(init-state id)])]])]))))
     [history []]
     foreign-deps)))

(defn add-records [history table new-records]
  (let [foreign-deps (mapcat find-foreign-deps new-records)
        [history ops] (satisfy-foreign-deps history foreign-deps)
        history (update-in history [table :remaining-ids] #(reduce disj % (map :xt/id new-records)))]
    [history (conj ops (insert-op table new-records))]))

(defn tx-add-stock [history]
  ;; find inventory for some film-ids, satisfy its films (and dependents like actors, actor films, categories, category films)
  (let [n-records (inc (rand-int 16))
        {:keys [inventory]} history
        {:keys [remaining-ids, init-state]} inventory
        new-records (map init-state (take n-records remaining-ids))]
    (add-records history :inventory new-records)))

(defn tx-new-customer [history]
  (let [{:keys [customer]} history
        {:keys [remaining-ids, init-state]} customer
        new-records (map init-state (take 1 remaining-ids))]
    (add-records history :customer new-records)))

(defn tx-start-rental [history]
  (let [{:keys [rental]} history
        {:keys [remaining-ids, init-state]} rental
        new-records (map init-state (take 1 remaining-ids))]
    ;; you want to defer returning the rental here by some time period?
    (add-records history :rental new-records)))

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
                   init-state (atom {})
                   _ (with-open [rdr (io/reader edn-file)
                                 pb-rdr (PushbackReader. rdr)]
                       (let [read-next (fn [] (edn/read {:eof nil, :readers {'time/instant #(Instant/parse %)}} pb-rdr))]
                         (loop []
                           (when-some [obj (read-next)]
                             (swap! init-state assoc (:xt/id obj) obj)
                             (recur)))))]]
         (case table-name
           ;; reference tables, do not see transactions (for now)
           ("city" "country" "language" "staff" "store" "category")
           [(keyword table-name)
            {:ref-state @init-state
             :init-state @init-state,
             :state @init-state,
             :remaining-ids #{}}]
           ;; other tables start empty and accrete over time
           [(keyword table-name)
            {:init-state @init-state,
             :remaining-ids (set (keys @init-state))
             :state {}}]))
       (into {})
       (satisfy-init-deps)))

(def start-time (Instant/parse "2023-01-01T00:00:00Z"))
(def end-time (Instant/parse "2024-02-05T00:00:00Z"))

(defn insert-into-node [node]
  (let [tx-time (Duration/parse "PT24H")
        [history transactions]
        (loop [time (.plus start-time tx-time)
               h (init-history)
               transactions []]
          (if (<= (compare end-time time) 0)
            [h transactions]
            (let [;; weights
                  gen-transaction (rand-nth [tx-add-stock])
                  [h operations] (gen-transaction h)]
              (recur (.plus time tx-time)
                     h
                     (conj transactions {:opts {:system-time time}
                                         :tx-ops (vec (mapcat :xt-dml operations))})))))]
    (doseq [[table {:keys [ref-state]}] history]
      (->> ref-state
           (sort-by key)
           (map val)
           (map (fn [row] (xt/put table row)))
           (partition-all 512)
           (run! (fn [puts] (xt/submit-tx node puts {:system-time start-time})))))

    (run! #(xt/submit-tx node (:tx-ops %) (:opts %)) transactions)))

(comment
  (def h (init-history))
  (keys h)
  (update-vals h (comp count :remaining-ids))
  (second (tx-new-customer h))
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
