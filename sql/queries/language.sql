-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a language by id"
SELECT *
FROM language
WHERE language.xt$id = ?