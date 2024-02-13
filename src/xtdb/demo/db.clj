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
        (xt/submit-tx node (for [line line-batch]
                             (xt/put table-name (edn/read-string {:readers {'time/instant #(Instant/parse %)}}
                                                                 line))))))))

(def xt-node
  (let [node (xtn/start-node {})]
    (doseq [file (sort (.listFiles (io/file "resources/sakila")))]
      (submit-file! node file))

    (log/info "Sakila playground started!")

    node))

(comment
  ;; `SELECT title, description, length FROM film WHERE title = 'APOCALYPSE FLAMINGOS'`
  (xt/q xt-node '(from :film [{:title "APOCALYPSE FLAMINGOS"} title description length]))

  (xt/q xt-node '(-> (unify (from :actor [{:xt/id actor-id, :first-name "TIM", :last-name "HACKMAN"}])
                            (from :film-actor [film-id actor-id])
                            (from :film [{:xt/id film-id} title]))
                     (return title)
                     (order-by title)
                     (limit 5)))

  #_(xt/q xt-node "select film.title from film_actor,film,actor where film_actor.film_id = film.xt$id and film_actor.actor_id = actor.xt$id group by film.title limit 10")
  ;;(xt/q xt-node "select * from film limit 10")

  ;; Have fun!
  )
