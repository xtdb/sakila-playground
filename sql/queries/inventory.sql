-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM inventory
WHERE inventory.xt$id = ?