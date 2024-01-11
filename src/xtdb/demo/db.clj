;; Copyright © 2023, JUXT LTD.

(ns xtdb.demo.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [xtdb.api :as xt]
            [xtdb.node :as xtn])
  (:import java.io.File
           java.time.Instant))

(defn submit-file! [node ^File file]
  (let [table-name (-> (.getName file)
                       (str/replace #"\.edn$" "")
                       (keyword))]
    (log/debugf "Submitting '%s' table" (name table-name))
    (with-open [rdr (io/reader file)]
      (doseq [line-batch (->> (line-seq rdr)
                             (partition-all 1000))]
        (xt/submit-tx node
          (for [line line-batch]
            (let [data (edn/read-string {:readers {'time/instant #(Instant/parse %)}}
                                        line)
                  valid-from (:xt/valid-from data)
                  valid-to (:xt/valid-to data)
                  enrich
                  (cond
                    (and valid-from valid-to)
                    #(xt/during % valid-from valid-to)
                    valid-from
                    #(xt/starting-from % valid-from)
                    valid-to
                    #(xt/until % valid-to)
                    :else
                    identity)]
              (-> (xt/put table-name data)
                  enrich))))))))

#_(defn submit-file! [node ^File file]
  (let [table-name (-> (.getName file)
                       (str/replace #"\.edn$" "")
                       (keyword))]
    (log/debugf "Submitting '%s' table" (name table-name))
    (with-open [rdr (io/reader file)]
      (doseq [line-batch (->> (line-seq rdr)
                              (partition-all 1000))]
        (xt/submit-tx node (for [line line-batch]
                             (xt/put table-name (edn/read-string {:readers {'time/instant #(Instant/parse %)}}
                                                                 line))))))))

;; Convert edn to json. Can't work with this until I figure out how to
;; signal bitemp coords and ids.
(comment
  (doseq [file (sort (filter #(.isFile %) (.listFiles (io/file "resources/sakila"))))]
    (let [table-name (-> (.getName file)
                         (str/replace #"\.edn$" "")
                         (keyword))
          outfile (io/file "resources/sakila/json" (str (name table-name) ".json"))]
      (.mkdirs (.getParentFile outfile))
      (log/debugf "Converting '%s' table" (name table-name))
      (with-open [rdr (io/reader file)
                  writer (io/writer outfile)]
        (doseq [line (line-seq rdr)]
          (let [edn-line (edn/read-string {:readers {'time/instant #(Instant/parse %)}} line)
                json-line (clojure.data.json/write-str edn-line)
                json-line (str/replace json-line "id" "xt$id")]
            (.write writer json-line)
            (.write writer "\n")))))))

(def xt-node
  (let [node (xtn/start-node {})]
    (log/info "Loading data into XTDB...")
    (doseq [file (sort (.listFiles (io/file "resources/sakila")))]
      (submit-file! node file))
    (log/info "Sakila playground started!")
    {:xt-node node
     :ids (atom (into {}
                      (for [table [:film :customer]
                            :let [from-clause (list 'from table '[{:xt/id id}])]]
                        [table (-> (xt/q node (list '-> from-clause '(aggregate {:next (+ (max id) 1)}))) first :next)])))}))

(defn next-id [table]
  (get (swap! (:ids xt-node) update table inc) table))

(defn q [& args]
  (apply xt/q (:xt-node xt-node) args))

(defn sql-op [& args]
  (xt/submit-tx (:xt-node xt-node) [(xt/sql-op (first args))]))

(comment
  ;; `SELECT title, description, length FROM film WHERE title = 'APOCALYPSE FLAMINGOS'`
  (xt/q (:xt-node xt-node) '(from :film [{:title "APOCALYPSE FLAMINGOS"} title description length]))

  (xt/q (:xt-node xt-node) '(-> (unify (from :actor [{:xt/id actor-id, :first-name "TIM", :last-name "HACKMAN"}])
                                       (from :film-actor [film-id actor-id])
                                       (from :film [{:xt/id film-id} title]))
                                (return title)
                                (order-by title)
                                (limit 5)))

  #_(xt/q (:xt-node xt-node) "select film.title from film_actor,film,actor where film_actor.film_id = film.xt$id and film_actor.actor_id = actor.xt$id group by film.title limit 10")
  ;;(xt/q (:xt-node xt-node) "select * from film limit 10")

  ;; Have fun!

  (xt/q (:xt-node xt-node)
        '(-> (unify (from :film [{:xt/id film-id} language-id])
                    (from :film-actor [film-id actor-id])
                    (from :actor [{:xt/id actor-id}])
                    (from :film-category [film-id category-id])
                    (from :category [{:xt/id category-id}])
                    (from :language [{:xt/id language-id}])
                    (from :inventory [{:xt/id inventory-id} store-id film-id])
                    (from :store [{:xt/id store-id}])
                    (from :rental [{:xt/id rental-id} customer-id staff-id inventory-id])
                    (from :customer [{:xt/id customer-id} address-id])
                    (from :address [{:xt/id address-id} city-id])
                    (from :city [{:xt/id city-id} country-id])
                    (from :country [{:xt/id country-id}])
                    (from :payment [{:xt/id payment-id} rental-id])
                    (from :staff [{:xt/id staff-id}]))
             (order-by {:val payment-id :dir :desc})
             (limit 1)))
  )
