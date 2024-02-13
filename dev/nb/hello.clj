(ns ^{::clerk/visibility {:code :hide}}
  nb.hello
  (:require [clojure.string :as str]
            [nextjournal.clerk :as clerk]
            [xtdb.demo.db :as db]))

;; ## Hello XTDB

^{::clerk/visibility {:code :hide, :result :hide}}
(defn q [& sql]
  (let [sql (str/join "\n" sql)]
    (clerk/col
      (clerk/code {::clerk/opts {:language "sql"}} sql)
      (clerk/table (db/q sql)))))

{::clerk/visibility {:code :hide}}

(q "SELECT *"
   "FROM customer"
   "LIMIT 10")

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clojure.java.browse/browse-url "http://localhost:7777")
  (clerk/show! 'nb.hello)
  )
