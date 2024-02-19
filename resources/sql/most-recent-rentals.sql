SELECT rental.customer_id as customer_id,
       customer.first_name as first_name,
       customer.last_name as last_name, 
       category.name as category_name,
       EXTRACT (YEAR FROM rental.rental_date) as rental_year,
       EXTRACT (MONTH FROM rental.rental_date) as rental_month,
       EXTRACT (DAY FROM rental.rental_date) as rental_day,
       EXTRACT (HOUR FROM rental.rental_date) as rental_hour,
       EXTRACT (YEAR FROM rental.return_date) as return_year,
       EXTRACT (MONTH FROM rental.return_date) as return_month,
       EXTRACT (DAY FROM rental.return_date) as return_day,
       EXTRACT (HOUR FROM rental.return_date) as return_hour
FROM rental
JOIN customer ON rental.customer_id=customer.xt$id
JOIN inventory ON rental.inventory_id=inventory.xt$id
JOIN film_category ON inventory.film_id=film_category.film_id
JOIN category ON film_category.category_id=category.xt$id
WHERE rental.return_date IS NOT NULL
    AND rental.rental_date >= COALESCE(?, rental.rental_date)
    AND rental.rental_date <= COALESCE(?, rental.rental_date)
ORDER BY rental.rental_date DESC
LIMIT 25