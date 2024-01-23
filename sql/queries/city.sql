-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a city by id"
SELECT *
FROM city
WHERE city.xt$id = ?