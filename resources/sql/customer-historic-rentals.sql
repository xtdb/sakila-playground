SELECT rental.xt$id as id,
       film.title,
       rental.rental_id, rental.customer_id, rental.inventory_id,
       rental.rental_date as rental_date,
       rental.return_date as return_date
FROM rental
LEFT JOIN customer ON (rental.customer_id = rental.xt$id)
LEFT JOIN inventory ON (rental.inventory_id = inventory.xt$id)
LEFT JOIN film ON (inventory.film_id = film.xt$id)
WHERE rental.customer_id = ? AND rental.return_date IS NOT NULL
ORDER BY rental.rental_date desc
