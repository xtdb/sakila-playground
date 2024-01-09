(ns basic-test
  (:require [clojure.test :as t]
            [user :refer [xt-node]]
            [xtdb.api :as xt]))

;; Try modifying some of these yourself!
(t/deftest basic-query
  ;; How long is "APOCALYPSE FLAMINGOS"?
  (t/is (= [{:length 119}]
           (xt/q xt-node
             '(from :film [{:title $film-name} length])
             {:args {:film-name "APOCALYPSE FLAMINGOS"}})))

  ;; How many films are being rented right now?
  (t/is (= [{:count 183}]
           (xt/q xt-node
             '(-> (from :rental [inventory-id])
                  (aggregate {:count (row-count)})))))

  ;; How many films have ever been rented?
  (t/is (= [{:count 16044}]
           (xt/q xt-node
             '(-> (from :rental {:bind [inventory-id]
                                 :for-valid-time :all-time})
                  (aggregate {:count (row-count)})))))

  ;; What is the most popular film rented right now?
  (t/is (= [{:title "CLUB GRAFFITI" :count 2}]
           (xt/q xt-node
             '(-> (unify (from :rental [inventory-id])
                         (from :inventory [{:xt/id inventory-id} film-id])
                         (from :film [{:xt/id film-id} title]))
                  (aggregate title {:count (row-count)})
                  (order-by {:val count :dir :desc} title)
                  (limit 1)))))

  ;; What is the most popular film rented ever?
  (t/is (= [{:title "BUCKET BROTHERHOOD" :count 34}]
           (xt/q xt-node
             '(-> (unify (from :rental {:bind [inventory-id]
                                        :for-valid-time :all-time})
                         (from :inventory [{:xt/id inventory-id} film-id])
                         (from :film [{:xt/id film-id} title]))
                  (aggregate title {:count (row-count)})
                  (order-by {:val count :dir :desc} title)
                  (limit 1)))))

  ;; Who last rented "JAWS HARRY"?
  (t/is (= [{:first-name "ARLENE", :last-name "HARVEY"}]
           (xt/q xt-node
             '(-> (unify (from :film [{:xt/id film-id, :title $film-name}])
                         (from :inventory [{:xt/id inventory-id} film-id])
                         (from :rental {:bind [inventory-id customer-id xt/valid-from]
                                        :for-valid-time :all-time})
                         (from :customer [{:xt/id customer-id} first-name last-name]))
                  (order-by {:val xt/valid-from :dir :desc})
                  (return first-name last-name)
                  (limit 1))
             {:args {:film-name "JAWS HARRY"}}))))

;; Not a fun/interesting query
;; Just checks that we can join on all the tables
(t/deftest e2e
  (t/is (not= []
              (xt/q xt-node
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
                        (limit 1))))))
