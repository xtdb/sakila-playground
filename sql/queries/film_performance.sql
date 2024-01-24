-- :category "Custom"
-- :desc "How well films are performing"
SELECT
  perf.film,
  perf.units,
  perf.revenue,
  perf.units * perf.replacement_cost AS stock_cost,
  perf.revenue / (perf.units * perf.replacement_cost) AS score
FROM
  (
    SELECT
      f.title AS film,
      f.replacement_cost,
      (SELECT COUNT(1) FROM inventory i WHERE i.film_id = f.xt$id) units,
      (SELECT SUM(p.amount)
       FROM inventory i
       JOIN rental FOR ALL VALID_TIME r ON r.xt$id = i.rental_id
       JOIN payment p ON p.rental_id = r.xt$id
       WHERE i.film_id = f.xt$id) revenue
    FROM film AS f
  )
AS perf
ORDER BY score DESC
