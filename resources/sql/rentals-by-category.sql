WITH rental_categories AS
     (SELECT film_category.category_id as category_id,
             count(*) as films_rented
      FROM rental FOR VALID_TIME FROM ? TO ?
      LEFT JOIN inventory ON rental.inventory_id = inventory.xt$id
      LEFT JOIN film_category ON inventory.film_id = film_category.film_id
      GROUP BY film_category.category_id)
SELECT category.name AS category_name,
       rental_categories.category_id,
       rental_categories.films_rented
FROM rental_categories
LEFT JOIN category ON rental_categories.category_id = category.xt$id
ORDER BY category.name ASC
