(ns xtdb.demo.web.data-test
  (:require
   [clojure.test :refer [deftest is]]
   [xtdb.demo.db :refer [xt-node]]
   [xtdb.api :as xt]))

(comment
  (xt/q
   (:xt-node xt-node)
   "select * from inventory "))

(comment
  {:xt$valid_from
   #time/zoned-date-time "2024-01-04T10:36:15.935976Z[UTC]",
   :xt$valid_to nil,
   :staff_id 2,
   :customer_id 528,
   :inventory_id 524,
   :xt$id 10084}

  {:xt$valid_from
   #time/zoned-date-time "2024-01-04T10:36:14.394949Z[UTC]",
   :address_id 9,
   :email "ELIZABETH.BROWN@sakilacustomer.org",
   :last_name "BROWN",
   :store_id 1,
   :first_name "ELIZABETH",
   :active true,
   :xt$id 5}

  {:store_id 1, :film_id 471, :xt$id "2178"})


;; XTQL
(comment
  (xt/q (:xt-node xt-node)
        '(unify (from :rental {:bind
                               [{:xt/id rental_id} {:customer-id $customer_id} inventory_id
                                {:xt/valid-from valid_from} {:xt/valid-to valid_to}]
                               :for-valid-time :all-time})
                (from :customer [{:xt/id $customer_id}])
                (from :inventory [{:xt/id inventory_id} film_id])
                (from :film [{:xt/id film_id} title]))
        {:args {:customer_id 560}
         :key-fn :snake_case}))

(comment
  (for [result
        (xt/q (:xt-node xt-node)
              "SELECT rental.xt$valid_from, rental.xt$valid_to, customer.first_name, customer.last_name, film.title FROM rental LEFT JOIN customer ON customer.xt$id = rental.customer_id LEFT JOIN inventory ON inventory.xt$id = rental.inventory_id LEFT JOIN film ON film.xt$id = inventory.film_id WHERE rental.customer_id = ?"
              {:args [100]})]
    result
    ))
