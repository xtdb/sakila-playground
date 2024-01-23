-- :params {:id :long}
-- :param-order [:id]
-- :desc "Select a category by id"
SELECT *
FROM category
WHERE category.xt$id = ?