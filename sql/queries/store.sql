-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM store
WHERE store.xt$id = ?