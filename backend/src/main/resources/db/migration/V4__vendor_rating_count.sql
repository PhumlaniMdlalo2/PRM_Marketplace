-- A rating average on its own is not a trust signal: 5.00 from one review and 5.00 from fifty look
-- identical on the listing page. The count is stored next to the average so the two can be read
-- together.
--
-- Existing profiles get 0 rather than a guessed number. Backfilling from the reviews table would be
-- wrong here: the average the code maintains covers a vendor's whole catalogue, and deriving it per
-- profile during a migration would silently disagree with what the application computes on the next
-- write. The first review filed against a profile settles it.
alter table vendor_profiles
    add column rating_count int not null default 0;