-- :category "Custom"
-- :desc "Monthly financial breakdown"
-- :col-order [:store_id, :month, :revenue]
SELECT s.xt$id store_id,
       m.mon AS month,
 -- sum of payments taken
   (SELECT
     COALESCE(FLOOR(SUM(p.amount)), 0.0)
    FROM payment p
    JOIN rental r ON r.xt$id = p.rental_id
    JOIN inventory i ON i.xt$id = r.inventory_id
    WHERE EXTRACT(MONTH FROM p.xt$valid_from) = m.mon
    AND i.store_id = s.xt$id) revenue
FROM store s, (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12)) AS m(mon)
GROUP BY s.xt$id, m.mon
ORDER BY s.xt$id, m.mon
