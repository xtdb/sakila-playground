-- :category "Custom"
WITH rentals_ym AS (
    SELECT EXTRACT (YEAR FROM rental.rental_date) as year,
           EXTRACT (MONTH FROM rental.rental_date) as month,
           rental.rental_date as ts
    FROM rental)
SELECT A.year, A.month, count(*) as rented, max(A.ts) as ts
FROM rentals_ym as A
GROUP BY A.year, A.month
ORDER BY A.year DESC, A.month DESC
