-- Reviews had a foreign key on the reviewer but not on the product, which every other table that
-- points at a product does have (product_images, cart_items, saved_items, order_items,
-- conversations). Without it a review can name a product id that does not exist, and deleting a
-- product leaves its reviews behind pointing at nothing.
--
-- Rows that point at a missing product are deleted rather than left in place: there is no product to
-- attach them to and no way to guess which one was meant. Reviews of a product that does exist are
-- untouched, including any that later get flagged by the purchase check in ReviewServiceImpl.
delete r
from reviews r
left join products p on p.id = r.product_id
where p.id is null;

alter table reviews
    add constraint fk_reviews_product foreign key (product_id) references products (id);