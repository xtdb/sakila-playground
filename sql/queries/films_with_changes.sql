-- :category "Custom"
-- :desc "Lists all films who have had been changed over time"
SELECT f2.film_id
FROM (SELECT f.xt$id film_id, COUNT(*) changes
      FROM film FOR ALL VALID_TIME f
      GROUP BY f.xt$id) f2
WHERE f2.changes > 1
