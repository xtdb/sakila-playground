-- :desc "List all inventory rows"
SELECT *
FROM inventory
ORDER BY inventory.xt$id
LIMIT 100