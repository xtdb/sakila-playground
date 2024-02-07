-- :category "Custom"
SELECT top_user.customer_id,
       top_user.films_rented,
       customer.last_name,
       customer.first_name,
       customer.email
FROM (SELECT rental.customer_id,
             count(*) AS films_rented
      FROM rental
      WHERE rental.return_date IS NOT NULL
      GROUP BY rental.customer_id
      ORDER BY films_rented DESC
      LIMIT 10) top_user
LEFT JOIN customer ON top_user.customer_id = customer.xt$id
