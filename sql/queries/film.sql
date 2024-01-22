-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM film
WHERE film.xt$id = ?