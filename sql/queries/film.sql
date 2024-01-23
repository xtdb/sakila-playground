-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a film by id"
SELECT *
FROM film
WHERE film.xt$id = ?