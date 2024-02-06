WITH film_rented AS 
    (SELECT inventory.film_id, 
            count(*) as count_rented
    FROM rental FOR VALID_TIME FROM ? TO ?
    LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
    GROUP BY inventory.film_id
    ORDER BY count_rented DESC
    LIMIT 10)
SELECT film_rented.film_id,
        film.title, 
        film_rented.count_rented 
FROM film_rented
LEFT JOIN film ON film_rented.film_id = film.xt$id