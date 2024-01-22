-- :params {:id :long}
-- :param-order [:id]
SELECT *
FROM category
WHERE category.xt$id = ?