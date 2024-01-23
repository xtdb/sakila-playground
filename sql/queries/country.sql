-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a country by id"
SELECT *
FROM country
WHERE country.xt$id = ?