-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM country
WHERE country.xt$id = ?