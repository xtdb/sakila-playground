-- :category "Custom"
-- :desc "Find customers who have been mistakenly charged the wrong amount for a return"
SELECT
  p.customer_id, p.xt$id payment_id
FROM payment p
JOIN rental r ON r.xt$id = p.rental_id
JOIN inventory i ON i.xt$id = r.inventory_id
WHERE p.amount <> (SELECT f.rental_rate FROM film FOR ALL VALID_TIME f WHERE f.xt$id = i.film_id AND f.xt$valid_time CONTAINS r.rental_date)
