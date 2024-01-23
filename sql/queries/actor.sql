-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a actor by id"
SELECT *
FROM actor
WHERE actor.xt$id = ?