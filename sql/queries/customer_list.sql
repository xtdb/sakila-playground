-- :desc "List all customer rows"
SELECT *
FROM customer
ORDER BY customer.xt$id
LIMIT 100