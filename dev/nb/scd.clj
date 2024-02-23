(ns nb.scd
  {:nextjournal.clerk/visibility {:code :hide, :result :hide}}
  (:require [nextjournal.clerk :as clerk]))

{::clerk/visibility {:code :hide, :result :hide}}

(defn tbl [& rows]
  (let [cols (first rows)]
    (clerk/col
      (clerk/table (map #(zipmap cols %) (rest rows))))))

{::clerk/visibility {:code :hide, :result :show}}

;; A common problem is modelling mutable data that serve as a reference dimension for facts.

;; Consider rentals.

(tbl
  ["rental_id" "customer_id" "rental_date" "inventory_id" "return_date" "created_at" "updated_at"]
  [42 12334 "2024-03-14" 366 "NULL" "2024-03-14T09:55:34Z" "2024-03-14T09:55:34Z" ])

;; The rental references a film record transitively through inventory.

(tbl
  ["inventory_id" "store_id" "film_id", "lost", "created_at", "updated_at"]
  [366, 3, 255, false, "2024-01-11T14:21:22Z" "2024-01-11T14:21:22Z"])

(tbl
  ["film_id" "title" "price" "created_at", "updated_at"]
  [255 "Jurassic Park", 3.99, "2020-11-01T15:14:19Z" "2024-01-15T13:14:59Z"])

;; In this example, we operate an online rental service, where charges are issued at the end of the month.

;; We might want to ask
;; > how much should we charge each customer for jan?

;; ```sql
;; SELECT r.customer_id, SUM(f.price) amount
;; FROM rental r
;; JOIN inventory i ON r.inventory_id = i.inventory_id
;; JOIN film f ON f.film_id = i.film_id
;; WHERE r.rental_date BETWEEN '2024-01-01' AND '2024-01-31'
;; GROUP BY r.customer_id
;; ```

(tbl
  ["customer_id", "amount"]
  [12334, 3.99])

;; Ah, but there was a price change on the 15th of january.

(tbl
  ["film_id" "title" "price" "created_at", "updated_at"]
  [255 "Jurassic Park", 2.99, "2020-11-01T15:14:19Z" "2020-12-17T12:38:45Z"])

;; We have a problem. The customer should have been charged `2.99`.

;; There are workarounds to this issue

;; ## Model the times at which a price applies

;; ```sql
;; SELECT r.customer_id, SUM(f.price) amount
;; FROM rental r
;; JOIN inventory i ON r.inventory_id = i.inventory_id
;; JOIN film f ON f.film_id = i.film_id
;; JOIN film_price fp ON fp.film_id = f.film_id AND r.rental_date BETWEEN fp.start_date AND fp.end_date
;; WHERE r.rental_date BETWEEN '2024-01-01' AND '2024-01-31'
;; GROUP BY r.customer_id
;; ```

;; Issues
;; - extra maintenance involved in price changes, you must not only insert a row, but update the existing current price
;;   to change its end date
;; - assumption of non overlapping ranges, if by accident or error there are overlapping prices for a date range
;;   you might get surprising results
;; - Needs code, triggers and so on to maintain - written by you.
;; - You must predict which tables need history like this ahead of time, if you discover a query that cannot be satisfied
;;   you will need to first change your schema or add to it to model history before you can answer the query.

;; ## Copy the price into the rental when the rental record is created

;; ```sql
;; SELECT r.customer_id, SUM(r.price) amount
;; FROM rental r
;; WHERE r.rental_date BETWEEN '2024-01-01' AND '2024-01-31'
;; GROUP BY r.customer_id
;; ```

;; The query is quite straightforward but:
;;
;; - Code or a trigger needs to exist to make copy the data. It needs to be correct.
;; - You must predict the need for this de-normalization ahead of time.
;; - You have limited capability to explain why the price is what it is

;; In this example, copying the price is probably satisfactory but this is a simple example.
;; You might have several such attributes changing at different times, transitively through many different relationships.
;; You end up with the same issues that normalization might address, such as complexity of changing the data, complexity in adding attributes or extending them model.

;; ## XTDB Bi-temporality

;; ```sql
;; SELECT r.customer_id, SUM(f.price) amount
;; FROM rental r
;; JOIN inventory i ON r.inventory_id = i.inventory_id
;; JOIN film FOR ALL VALID_TIME f ON f.film_id = i.film_id
;; WHERE r.rental_date BETWEEN '2024-01-01' AND '2024-01-31'
;; AND f.valid_time OVERLAPS PERIOD(r.rental_date, r.rental_date)
;; GROUP BY r.customer_id
;; ```

;; Using valid time, we are able to query the films as of some prior time, without the addition of history tables or other schema changes.

;; - This lets us maintain a straightforward schema, where the price can be freely changed.
;; - We could drop the `created_at`/`updated_at` columns as they are implicit in the temporal model
;; - We do not have to predict ahead of time which attributes or entities need history to satisfy business requirements.
;;   Any future need for history is immediately met.
;; - Furthermore, erroneous history, where the facts as recorded do not reflect the facts as they-were (e.g a misrecording of price) can be corrected in the past
;;   as the entries associated with valid time are user editable.
;; - Valid time also lets you specify futures, not just histories. For example - if you know a price will change next month, you could enter the change in valid time - and see your projections
;;   pick up the correct price value.


^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  ;; ideally
  ;; SELECT r.customer_id, SUM(f.price) amount
  ;; FROM rental r
  ;; JOIN inventory i ON r.inventory_id = i.inventory_id
  ;; JOIN film FOR VALID_TIME AS OF r.rental_date f AND f.film_id = i.film_id
  ;; WHERE r.rental_date BETWEEN '2024-01-01' AND '2024-01-31'
  ;; GROUP BY r.customer_id
  )

^{::clerk/visibility {:code :hide, :result :hide}}
(comment
  (clerk/show! 'nb.scd)
  ((requiring-resolve 'clojure.java.browse/browse-url) "http://localhost:7777")
  )
