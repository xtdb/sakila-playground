-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a customer by id"
SELECT *
FROM customer
WHERE customer.xt$id = ?