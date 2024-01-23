-- :category "Custom"
-- :desc "Films currently available at each store"
-- :col-order [:store_id, :title, :qty]
SELECT
 s.xt$id store_id,
 f.title,
 COUNT(i.xt$id) qty
FROM store s
JOIN inventory i ON i.store_id = s.xt$id
JOIN film f ON f.xt$id = i.film_id
-- NOT EXISTS seems very slow...
LEFT JOIN rental r ON r.inventory_id = i.xt$id
WHERE r.xt$id IS NULL
GROUP BY s.xt$id, f.title
ORDER BY store_id, title
