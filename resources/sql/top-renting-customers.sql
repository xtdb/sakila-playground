WITH top_user AS 
    (SELECT rental.customer_id, 
            count(*) AS films_rented
    FROM rental 
    WHERE rental.xt$valid_from >= COALESCE(?, rental.xt$valid_from) 
        AND 
        rental.xt$valid_from<=COALESCE(?, rental.xt$valid_from)
    GROUP BY rental.customer_id
    ORDER BY films_rented DESC
    LIMIT 10)
SELECT top_user.customer_id, 
        top_user.films_rented,
        customer.last_name,
        customer.first_name,
        customer.email            
FROM top_user
LEFT JOIN customer ON top_user.customer_id = customer.xt$id