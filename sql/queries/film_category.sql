-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a film_category by id"
SELECT *
FROM film_category
WHERE film_category.xt$id = ?