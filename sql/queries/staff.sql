-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a staff by id"
SELECT *
FROM staff
WHERE staff.xt$id = ?