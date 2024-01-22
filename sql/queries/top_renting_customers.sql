SELECT top_user.customer_id,
       top_user.films_rented,
       customer.last_name,
       customer.first_name,
       customer.email
FROM (SELECT rental.customer_id,
             DATE_DIFF(rental.xt$valid_to, rental.xt$valid_from, 'DAY')
             count(*) AS films_rented
      FROM rental FOR ALL VALID_TIME
      GROUP BY rental.customer_id
      ORDER BY films_rented DESC
      LIMIT 10) top_user
LEFT JOIN customer ON top_user.customer_id = customer.xt$id
