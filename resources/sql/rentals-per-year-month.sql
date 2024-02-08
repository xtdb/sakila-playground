WITH rentals_ym AS (
    SELECT EXTRACT (YEAR FROM rental.xt$valid_from) as year,
           EXTRACT (MONTH FROM rental.xt$valid_from) as month
    FROM rental
    WHERE rental.xt$valid_from >= COALESCE(?, rental.xt$valid_from) 
          AND 
          rental.xt$valid_from<=COALESCE(?, rental.xt$valid_from))
SELECT A.year, A.month, count(*) as rented
FROM rentals_ym as A
GROUP BY A.year, A.month
ORDER BY A.year DESC, A.month DESC