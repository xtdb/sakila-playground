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
(defn rand-nth [coll] (nth coll (rand-int (count coll))))
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
        history (update-in history [table :remaining-ids] #(reduce disj % (map :xt/id new-records)))]
    [history (conj ops (insert-op table new-records))]))

(defn tx-add-stock [history]
  ;; find inventory for some film-ids, satisfy its films (and dependents like actors, actor films, categories, category films)
  (let [n-records (inc (rand-int 16))
        {:keys [inventory]} history
        {:keys [remaining-ids, sample-state]} inventory
        new-records (map sample-state (take n-records remaining-ids))]
    (add-records history :inventory new-records)))

(defn tx-new-customer [history]
  (let [{:keys [customer]} history
        {:keys [remaining-ids, sample-state]} customer
        new-records (map sample-state (take 1 remaining-ids))]
    (add-records history :customer new-records)))

(defn tx-start-rental [history]
  (let [{:keys [rental]} history
        {:keys [remaining-ids, sample-state]} rental
        new-records (map sample-state (take 1 remaining-ids))]
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
                   file-state (atom {})
                   _ (with-open [rdr (io/reader edn-file)
                                 pb-rdr (PushbackReader. rdr)]
                       (let [read-next (fn [] (edn/read {:eof nil, :readers {'time/instant #(Instant/parse %)}} pb-rdr))]
                         (loop []
                           (when-some [obj (read-next)]
                             (swap! file-state assoc (:xt/id obj) obj)
                             (recur)))))]]
         (case table-name
           ;; reference tables, do not see transactions (for now)
           ("city" "country" "language" "staff" "store" "category")
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
(def tx-time (Duration/parse "PT24H"))

(def transactions-weighted
  ;; poor mans weighted sampler, I have code for an alias sampler but would need to bring in a minmaxpriorityqueue dep
  (->> {#'tx-add-stock 1
        #'tx-new-customer 2}
       (mapcat (fn [[f weight]] (repeat weight f)))
       vec))

(defn generate-transactions [history]
  (loop [time (.plus start-time tx-time)
         h history
         transactions []]
    (if (<= (compare end-time time) 0)
      transactions
      (let [;; todo weights
            gen-transaction (rand-nth transactions-weighted)
            [h operations] (gen-transaction h)]
        (recur (.plus time tx-time)
               h
               (conj transactions {:opts {:system-time time}
                                   :tx-ops (vec (mapcat :xt-dml operations))}))))))

(defn setup-node [node seed]
  (binding [*rng* (Random. seed)]
    (let [initial-history (init-history)
          transactions (generate-transactions initial-history)]
      (doseq [[table {:keys [init-state]}] initial-history]
        (->> init-state
             (sort-by key)
             (map val)
             (map (fn [row] (xt/put table row)))
             (partition-all 512)
             (run! (fn [puts] (xt/submit-tx node puts {:system-time start-time})))))
      (run! #(xt/submit-tx node (:tx-ops %) (:opts %)) transactions))))

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
