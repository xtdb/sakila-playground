-- :category "Custom"
-- :desc "How well categories perform relative to the inventory cost"
SELECT
  perf.category,
  perf.revenue / perf.cost AS score
FROM
  (SELECT c.name category,
       -- revenue of all films that are tagged in the category
       (SELECT SUM(p.amount)
        FROM payment p
        JOIN rental r ON p.rental_id = r.xt$id
        JOIN inventory i ON r.inventory_id = i.xt$id
        WHERE i.film_id = ANY (SELECT fc.film_id FROM film_category fc WHERE fc.category_id = c.xt$id)) revenue,
       -- cost of all films tagged in the category
       (SELECT SUM(f.replacement_cost)
        FROM inventory i
        JOIN film f ON f.xt$id = i.film_id
        WHERE i.film_id = ANY (SELECT fc.film_id FROM film_category fc WHERE fc.category_id = c.xt$id)) cost
   FROM category c)
  AS perf
ORDER BY score DESC
