-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a rental by id"
SELECT *
FROM rental
WHERE rental.xt$id = ?