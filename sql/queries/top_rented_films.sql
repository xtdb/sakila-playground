-- :desc "The most rented films"
-- :category "Custom"
SELECT
  film.xt$id film_id,
  film.title,
  COUNT(*) rental_count
FROM rental
LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
JOIN film ON film.xt$id = inventory.film_id
GROUP BY film.xt$id, film.title
ORDER BY rental_count DESC
LIMIT 10
