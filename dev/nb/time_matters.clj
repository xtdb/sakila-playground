(ns nb.time-matters
  {:nextjournal.clerk/visibility {:code :hide, :result :hide}}
  (:require [nextjournal.clerk :as clerk]))

{::clerk/visibility {:code :hide, :result :hide}}

(defn tbl [& rows]
  (let [cols (first rows)]
    (clerk/col
      (clerk/table (map #(zipmap cols %) (rest rows))))))

{::clerk/visibility {:code :hide, :result :show}}

;; ---
;; ## XTDB: Safety, Power and Freedom, with Time-Travel

;; ### Times Matters - safety-first development

;; XTDB is a time-travel database designed for building complex applications, and to support developers with features that are essential for meeting common regulatory compliance requirements.

;; Temporal querying has been identified as a hard problem in SQL database for decades, and XTDB is the first ground-up realization of the 'bitemporal model', but XTDB's temporal powers have benefits for all kinds of applications - not just advanced scenarios in specific domains.

;; There are three layers of concepts that we'll discuss:
;; - "Basis" - using a timestamp as a stable basis for querying prior database states - an essential safety net for developers and organizations alike - and the key to consistent downstreaming processing (e.g. creating a Financial Report)
;; - "System-Time" - a pair of hidden timestamp columns built-in to every table, maintained automatically, that can be used to retrieve old versions of data
;; - "Valid-Time" - an advanced versioning mechanism that is helpful for scheduling changes across data, and critical for time-ware applications where system-time alone is not enough

;; First and foremost, XTDB is built to reduce the risks and impact of data loss, update anomalies and brittle database designs. It achieves this goal primarily by always recording the history of all changes (particularly UPDATEs and DELETEs which are normally destructive operations), and by restricting the scope of concurrent database usage, such that an auditable, linear sequence of all changes is retained.

;; Beyond the obvious auditing and debugging benefits of retaining change data and prior database states, XTDB's history-preserving capability presents a robust & stable source of truth within a wider IT architecture that is unlike anything that most databases can offer.

;; ### Basis - the key to consistent downstream processing (e.g. financial reporting)

;; In an ideal world a single database system can handle all compute tasks that an application may have, but in practice it is often necessary for external processes to coordinate and operate over a stable snapshot of the database in order to produce accurate and consistent outputs.

;; Finanical reports are a classic example where many independent analyses over the database must take place over a number of days or weeks, and yet somehow be consistent as-of a fixed reporting period such that the final compiled report, with all its statements and KPIs, is as accurate as possible.

;; The traditional solution to this requirement is to take a full copy of the database which becomes read-only and can be interrogated independently of the production database. However, this approach typically requires extensive resource consumption, delays any downstream processes from starting until the copying is complete, and also demands foreknowledge of the relevant moment in time at which to create the copy.

;; In contrast, XTDB allows you to query the entire database as-of any previous database state using just a timestamp - a 'basis' - and to re-run queries against a single basis to arrive at consistent, repeatable results based on the same, stable set of data.

;; This principle closely follows Rich Hickey's description of the "Epochal Time Model" - as pioneered by Clojure and Datomic - where the database follows a succession of atomic state transitions in response to a series of transactions, and allows multiple observers to safely reference all prior states without conflict or contention. Recommended viewing (particularly the section on 'Memory' at 34:17):

(clerk/html [:iframe {:width "708"
                      :height "398"
                      :src "https://www.youtube.com/embed/ROor6_NGIWU?t=2057"
                      :title "The Language of the System - Rich Hickey"
                      :frameborder "0"
                      :allow "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                      :allowfullscreen true}])

;; ### Basis - "lock free" observation across any prior database states without expensive snapshots or copying

;; Unlike a snapshot or full backup, XTDB's timestamp-derived basis is a lightweight concept that alleviates traditional concerns about copying (storage costs, and latency) or locking (impact on concurrent readers/writers).

;; XTDB achieves this by separating reads and writes via a single, fully-ordered log of all transactions. This allows developers to reasons about discrete database states as a linear sequence and avoid the risks of hitting anomalies that are inherent with more complex concurrency models.

;; Even better, prior states can be accessed and compared entirely within SQL queries, without leaving the operational context of the system (e.g. unlike restoring a database backup and replaying WAL files).

;; Like version control helps developers make sense of code, this immutable approach to tracking prior database states can help developers make sense of data, and be confident that nothing potentially important will get forgotten accidentally.

;; Keeping a history of all changes around in the database feels like working with git on a big project - everyone feels safer when version control is in place, and debugging becomes tractible.

;; XTDB's notion of basis supports many distinct scenarios:
;; - Access a past point in time to reproduce an issue
;; - Recover lost data from a past point in time
;; - Compare multiple, stable points in time without leaving SQL
;; - Unbounded repeatable reads against global snapshots, without locking, and outside of a running transaction - ideal for certains kinds of scalability

;; ---
;; ### System-Time

;; The mechanism underpinning XTDB's basis concept is called "system-time". The SQL:2011 specification formalized the notion, but it is often approximated in user schemas manually and comes in the form of "created-at" columns and audit tables (e.g. having both a "price" table and a "price_history" table).

;; Unlike XTDB, most databases don't offer system-time as a native concept, and where they do it still requires manual schema definition, and that in turn complicates schema evolution.

;; For example, given a set of product prices:

(tbl
 ["product_id" "product_price"]
 [42 10.35]
 [44 12.80]
 [57 6.99])

;; You can use system-time to validate the price that a customer would have been shown at a specific time in the past, while debugging a customer support issue. You can read the `system-time-to` for a deleted product to see what the final price was before the product was discontinued, but while it was still relevant for the financial reporting period.

;; Reference data that originates from outside your system (upstream sources) usually has a timestamp - valid time gives you a formal, universal place to record that timestamp.

;; ### Decisions, Transactions, Evidence

;; General purpose databases can turn their hands to almost anything. Postgres has been tremendously successful. New databases are generally built to accelerate workloads that a system like Postgres can't handle. For instance TigerBeetle is a fellow upstart database contender with a pure focus on ledger accounting workloads. And Clickhouse is focused on real-time analytical workloads. And DuckDB is focussed on single-machine workloads.

;; In XTDB's case, this workload is rigorous timestamp-versioning of reference data for transactional applications which are subject to regulatory requirements, where auditing around high-impact decision making is essential. In an investment bank, for instance, it is critical to be able to quickly determine what information a trading desk was aware of about the recent state of the world, and exactly when that information came to be known prior to a decision being made and a new transaction (e.g. financial trade) being created.

;; Both auditors who are reviewing trades for evidence of financial crime, or risk analysts who are reviewing trades for optimization of algorithms, require an accurate ability to answer: "what information did a trader, via the trading system, know about recent market events (i.e. a chronological understanding), and when did they find out that information?". Recording this information for every piece of "reference data" (i.e. data consumed from upstream sources) requires capturing - at a minimum - two timestamps. The system-time is the time when a new fact entered the system, and the valid-time is the real-world time at which the fact was believed true (e.g. Trade #123 was agreed via the London Stock Exchange at 11:08:02.213 UTC). All information associated withe trade is also recorded at this timestamp.

;; Information about this trade may not arrive into the system until 11:08:03.241.

;; It may also then be essential to record the believed impact of trade on the portfolio of a trading desk, and record this information slightly afterwards, once the complex risk calculations have completed at 11:10:03.592.

;; Outside of high-stakes domains, like finance or healthcare systems, the need for such precise record-keeping is somewhat less obvious, but still it is useful to consider the long-term benefits of the bitemporal model for decision makers and IT architecture more generally. After all, one persons transactions is another persons reference data.

;; Being able to easily review how and why decisions - transactions - get made is essential for improving future decision making.

;; For example, you can interrogate the database by scanning the timeline (in either direction, or 1:1) of "what did you know", and "when did you know it" (corrections/effectivity is a bonus).

;; ### Transactional decision support and the case for HTAP

;; Many architectures push analysis and review to systems that are fundamentally disconnect from the transactional sources. In some circumstances this can be benefical or even necessary, but XTDB contends that there are many circumstances where this is completely unnecessary. In the same way as Postgres will happily store reference data and transaction records, XTDB will handle a complex history of reference data and transaction records with strong explanatory power.


;; ---
;; ### Leaning on System-Time

;; Given history is being recorded anyway, it can be tempting to use the history as stored by system-time for performing analysis (i.e. beyond debugging, auditing, stable reporting, data recovery). This is possible but comes with complications.

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
