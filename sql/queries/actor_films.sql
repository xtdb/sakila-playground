-- :desc "List the films an actor has been in"
-- :col-order [:last_name, :first_name, :films]
SELECT
    a.last_name,
    a.first_name,
    ARRAY_AGG(f.title) films
FROM actor a
LEFT JOIN film_actor fa ON fa.actor_id = a.xt$id
LEFT JOIN film f ON f.xt$id= fa.film_id
GROUP BY a.xt$id, a.first_name, a.last_name
ORDER BY last_name, first_name
