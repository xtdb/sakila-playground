WITH rentals_ym AS (
    SELECT EXTRACT (YEAR FROM rental.xt$valid_from) as year,
           EXTRACT (MONTH FROM rental.xt$valid_from) as month,
           rental.xt$valid_from as ts
    FROM rental)
SELECT A.year, A.month, count(*) as rented, max(A.ts) as ts
FROM rentals_ym as A
GROUP BY A.year, A.month
ORDER BY A.year DESC, A.month DESC