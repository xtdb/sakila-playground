WITH film_rented AS 
    (SELECT inventory.film_id, 
            count(*) as count_rented
    FROM rental 
    LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
    WHERE rental.xt$valid_from >= COALESCE(?, rental.xt$valid_from) 
        AND 
        rental.xt$valid_from<=COALESCE(?, rental.xt$valid_from)
    GROUP BY inventory.film_id
    ORDER BY count_rented DESC
    LIMIT 10)
SELECT film_rented.film_id,
        film.title, 
        film_rented.count_rented 
FROM film_rented
LEFT JOIN film ON film_rented.film_id = film.xt$id