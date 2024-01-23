-- :category "Custom"
-- :desc "Shows a contact list for overdue rentals"
SELECT
  customer.xt$id customer_id,
  customer.first_name,
  customer.last_name,
  address.phone,
  film.xt$id film_id,
  film.title,
  rental.xt$valid_from rental_date
FROM rental
JOIN customer ON rental.customer_id = customer.xt$id
JOIN address ON customer.address_id = address.xt$id
JOIN inventory ON rental.inventory_id = inventory.xt$id
JOIN film ON inventory.film_id = film.xt$id
WHERE rental.xt$valid_from + film.rental_duration DAY < CURRENT_DATE
ORDER BY rental_date
