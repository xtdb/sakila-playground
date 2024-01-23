-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a address by id"
SELECT *
FROM address
WHERE address.xt$id = ?