-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM actor
WHERE actor.xt$id = ?