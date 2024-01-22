-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM customer
WHERE customer.xt$id = ?