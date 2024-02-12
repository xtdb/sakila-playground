-- :category "Custom"
-- :desc "Rentals that were entered as returned in the past"
SELECT rental.xt$id rental_id, rental.xt$valid_from, rental.xt$system_from, rental.return_date
FROM rental
WHERE rental.xt$valid_from < rental.xt$system_from
LIMIT 100
