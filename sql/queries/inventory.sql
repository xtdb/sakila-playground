-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a inventory by id"
SELECT *
FROM inventory
WHERE inventory.xt$id = ?