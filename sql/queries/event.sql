-- :category "Custom"
-- :desc "List all interesting events"
SELECT *
FROM event
ORDER BY event.st DESC
LIMIT 100
