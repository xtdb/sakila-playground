SELECT rental.xt$id as id, film.title, rental.rental_id, rental.customer_id, rental.inventory_id, rental.xt$valid_from as rental_date, rental.xt$valid_to as return_date
FROM rental FOR ALL VALID_TIME
LEFT JOIN customer ON (rental.customer_id = rental.xt$id)
LEFT JOIN inventory ON (rental.inventory_id = inventory.xt$id)
LEFT JOIN film ON (inventory.film_id = film.xt$id)
WHERE rental.customer_id = ?
ORDER BY rental.xt$valid_from desc
