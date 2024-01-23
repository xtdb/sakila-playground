-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a payment by id"
SELECT *
FROM payment
WHERE payment.xt$id = ?