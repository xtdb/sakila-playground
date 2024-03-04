(ns nb.future-price
  {:nextjournal.clerk/visibility {:code :hide, :result :hide}}
  (:require [nextjournal.clerk :as clerk]))

{::clerk/visibility {:code :hide, :result :hide}}

(defn tbl [& rows]
  (let [cols (first rows)]
    (clerk/col
      (clerk/table (map #(zipmap cols %) (rest rows))))))

{::clerk/visibility {:code :hide, :result :show}}

;; ---
;; ## Globally scheduled updates to relational data

;; A common problem is coordinating scheduled changes to data across a database without relying on mechanisms that are external to the database.

;; Consider a table of product prices.

;; How do we make sure that new prices are available to browse and purchase immediately at midnight on Black Friday?

;; Leaning on batch-oriented approaches like scheduling a cron job or a periodically refreshed materialized view is brittle and lacks proper control over timing.

;; One solution is to add a valid_from field and take the MAX that is less than the CURRENT_TIMESTAMP

;; However if we also want to coordinate this price change with a promotional offer and an update to a product description, it quickly becomes apparent that the requirement leaks across all queries.

;; With XTDB, we can push this cross-cutting requirement into the background and let the database handle things by leaning on the validity modelling that XTDB tracks for every row across every table. This allows you to INSERT "into the future" - effectively scheduling arbitrary changes to data:

;; Similarly sheduling of DELETE, end of effectivity -- session timeout (time-bound business process where a token has an expiry)

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
;; WHERE r.rental_date BETWEEN DATE '2024-01-01' AND DATE '2024-01-31'
;; GROUP BY r.customer_id
;; ```

(tbl
  ["customer_id", "amount"]
  [12334, 3.99])

;; Ah, but there was a price change on the 15th of january. Here is the previous state of the film row:

(tbl
  ["film_id" "title" "price" "created_at", "updated_at"]
  [255 "Jurassic Park", 2.99, "2020-11-01T15:14:19Z" "2020-12-17T12:38:45Z"])

;; We have a problem. The customer should be charged `2.99`, as that was the listed price at the time the rental was taken out.
;; Now we will contrast a few different solutions to this problem:

;; ## Copy the price into the rental when the rental record is created

;; ```sql
;; SELECT r.customer_id, SUM(r.price) amount
;; FROM rental r
;; WHERE r.rental_date BETWEEN DATE '2024-01-01' AND DATE '2024-01-31'
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

;; ## Model the times at which a price applies

;; If we created a film_price table, we might provide a `start_date` and `end_date` at which a price applies.

(tbl
  ["film_id" "price" "start_date", "end_date"]
  [255, 2.99, "2020-11-01T15:14:19Z" "2020-12-17T12:38:45Z"]
  [255, 3.99, "2024-01-15T13:14:59Z" "NULL"])

;; > *note* One could omit the end date and derive it as a view, as the prices are continuous (there are no gaps).

;; ```sql
;; SELECT r.customer_id, SUM(f.price) amount
;; FROM rental r
;; JOIN inventory i ON r.inventory_id = i.inventory_id
;; JOIN film f ON f.film_id = i.film_id
;; JOIN film_price fp ON fp.film_id = f.film_id AND r.rental_date BETWEEN fp.start_date AND COALESCE(fp.end_date, DATE '9999-12-31')
;; WHERE r.rental_date BETWEEN DATE '2024-01-01' AND DATE '2024-01-31'
;; GROUP BY r.customer_id
;; ```

;; - Extra maintenance involved in price changes, you must not only insert a row, but update the existing current price
;;   to change its end date
;; - Assumption of non overlapping ranges, if by accident or error there are overlapping prices for a date range
;;   you might get surprising results
;; - Needs code, triggers and so on to maintain - written by you.
;; - You must predict which tables need history like this ahead of time, if you discover a query that cannot be satisfied
;;   you will need to first change your schema or add to it to model history before you can answer the query.

;; ## XTDB Bi-temporality

;; ```sql
;; SELECT r.customer_id, SUM(f.price) amount
;; FROM rental r
;; JOIN inventory i ON r.inventory_id = i.inventory_id
;; JOIN film FOR ALL VALID_TIME f ON f.film_id = i.film_id
;; WHERE r.rental_date BETWEEN '2024-01-01' AND '2024-01-31'
;; AND f.valid_time CONTAINS PERIOD(r.rental_date, r.rental_date)
;; GROUP BY r.customer_id
;; ```

;; Using valid time, we are able to query the films as of some prior time, without the addition of history tables or other schema changes.

;; - This lets us maintain a straightforward schema, where the price can be freely changed.
;; - We could drop the `created_at`/`updated_at` columns as they are implicit in the temporal model
;; - We do not have to predict ahead of time which attributes or entities need history to satisfy business requirements.
;;   Any future need for history is immediately met.
;; - There are fewer derived inputs to the query, whose value could be incorrect due to say a bug.
;; - Furthermore, erroneous history, where the facts as recorded do not reflect the facts as-they-were (e.g a misrecording of price) can be corrected in the past
;;   as the entries associated with valid time are user editable.
;; - Valid time also lets you specify futures, not just histories. For example - if you know a price will change next month, you could enter the change in valid time - and see your projections
;;   pick up the correct price value.

;; ## Valid time corrections

;; One ability alluded to prior was to edit historical data in order to correct mistakes.
;; for example, suppose the price was mistakenly set to 399.0 by mistake due to a data entry error.

(tbl
  ["film_id" "title" "price"]
  [255 "Jurassic Park", 399.0])

;; a day later, we add a new price to the table fixing the problem, and send out an email to customers who have recently rented
;; the film to explain the pricing was in error.

(tbl
  ["film_id" "title" "price"]
  [255 "Jurassic Park", 3.99])

;; In a bi-temporal database, the mistaken state is preserved by default.
;; A problem arises, customers who rented the film while the price was incorrect will be charged the mistaken value.
;;
;; To remedy this, we can issue an update in the past.

;; ```sql
;; UPDATE film
;; FOR PORTION OF VALID_TIME FROM TIMESTAMP '2024-01-14 00:00:00' TO NULL
;; SET price = 3.99
;; WHERE film_id = 255
;; ```

;; If you then find the film price history, you will find it is corrected in valid time.
;; ```sql
;; SELECT * FROM film FOR ALL VALID_TIME WHERE film.film_id = 255
;; ```

(tbl
  ["film_id" "title" "price" "xt$valid_from" "xt$valid_to"]
  [255 "Jurassic Park", 2.99, "2020-11-01T15:14:19Z", "2024-03-14T00:00:00Z"]
  [255 "Jurassic Park", 3.99, "2024-03-14T00:00:00Z", "NULL"])

;; XTDB always remembers, so if you wish to view the prior uncorrected state, you can do so by manipulating `SYSTEM_TIME`.

;; ```sql
;; SELECT *
;; FROM film
;; FOR ALL VALID_TIME
;; FOR SYSTEM_TIME AS OF TIMESTAMP '2024-01-15 13:14:58'
;; WHERE film_id = 255
;; ```

(tbl
  ["film_id" "title" "price" "xt$valid_from" "xt$valid_to"]
  [255 "Jurassic Park", 2.99, "2020-11-01T15:14:19Z", "2024-01-15T13:14:59Z"]
  [255 "Jurassic Park", 399.0, "2024-01-15T13:14:59Z", "NULL"])

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
