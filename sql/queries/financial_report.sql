-- :category "Custom"
-- :desc "Monthly financial breakdown"
-- :col-order [:month, :rev23, :rev24]
SELECT m.mon AS month,
       (SELECT
         COALESCE(FLOOR(SUM(p.amount)), 0.0)
       FROM payment p
              JOIN rental r ON r.xt$id = p.rental_id
              JOIN inventory i ON i.xt$id = r.inventory_id
       WHERE EXTRACT(MONTH FROM p.payment_date) = m.mon
         AND EXTRACT(YEAR FROM p.payment_date) = 2023) rev23,
       (SELECT
         COALESCE(FLOOR(SUM(p.amount)), 0.0)
       FROM payment p
              JOIN rental r ON r.xt$id = p.rental_id
              JOIN inventory i ON i.xt$id = r.inventory_id
       WHERE EXTRACT(MONTH FROM p.payment_date) = m.mon
         AND EXTRACT(YEAR FROM p.payment_date) = 2024) rev24
FROM (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12)) AS m(mon)
GROUP BY m.mon
ORDER BY m.mon
