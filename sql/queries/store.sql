-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a store by id"
SELECT *
FROM store
WHERE store.xt$id = ?