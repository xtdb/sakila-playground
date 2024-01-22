-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM film_actor
WHERE film_actor.xt$id = ?