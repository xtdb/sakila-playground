-- :col-order [:xt/id :first-name :last-name :email :address-id :store-id]
-- :refs {:address-id "address.sql"}
SELECT *
FROM customer
ORDER BY xt$id
