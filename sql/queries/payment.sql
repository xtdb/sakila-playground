-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM payment
WHERE payment.xt$id = ?