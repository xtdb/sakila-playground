-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM staff
WHERE staff.xt$id = ?