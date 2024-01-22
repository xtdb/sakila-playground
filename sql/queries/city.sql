-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM city
WHERE city.xt$id = ?