-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM film_category
WHERE film_category.xt$id = ?