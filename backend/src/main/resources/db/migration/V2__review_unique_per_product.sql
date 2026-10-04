-- One review per product per person. The service relies on this index to reject a duplicate
-- submission instead of letting a race create two rows, which is what makes the "already reviewed"
-- check meaningful.
--
-- Duplicates are collapsed first so the migration succeeds on any database that predates the
-- constraint; the oldest row for each pair is kept and the others are deleted.
delete newer
from reviews newer
join reviews older
    on newer.product_id = older.product_id
    and newer.reviewer_id = older.reviewer_id
    and newer.id > older.id;

alter table reviews
    add constraint uk_review_product_reviewer unique (product_id, reviewer_id);