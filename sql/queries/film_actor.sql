-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a film_actor by id"
SELECT *
FROM film_actor
WHERE film_actor.xt$id = ?