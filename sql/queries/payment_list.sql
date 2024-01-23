-- :desc "List all payment rows"
SELECT *
FROM payment
ORDER BY payment.xt$id
LIMIT 100