-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM address
WHERE address.xt$id = ?