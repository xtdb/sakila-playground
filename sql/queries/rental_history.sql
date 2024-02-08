-- :category "Custom"
-- :params {:id :long}
-- :defaults {:id 88}
-- :param-order [:id]
-- :desc "History of a particular tape"
-- :col-order [:title :last_name :first_name, :rental_date, :return_date]
SELECT
  f.title,
  c.last_name,
  c.first_name,
  r.rental_date,
  r.return_date
FROM inventory i
JOIN rental r ON r.inventory_id = i.xt$id
JOIN film f ON f.xt$id = i.film_id
JOIN customer c ON c.xt$id = r.customer_id
WHERE i.xt$id = ?
ORDER BY return_date DESC
