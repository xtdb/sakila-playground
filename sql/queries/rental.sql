-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM rental
WHERE rental.xt$id = ?