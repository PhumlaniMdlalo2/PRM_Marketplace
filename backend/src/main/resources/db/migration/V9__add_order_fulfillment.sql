ALTER TABLE orders
    ADD COLUMN fulfillment_method ENUM('DELIVERY', 'MEETUP') NOT NULL DEFAULT 'MEETUP',
    ADD COLUMN estimated_delivery_date DATE NULL;

UPDATE orders
SET fulfillment_method = CASE
        WHEN shipping_address_id IS NULL THEN 'MEETUP'
        ELSE 'DELIVERY'
    END,
    estimated_delivery_date = CASE
        WHEN shipping_address_id IS NULL THEN NULL
        ELSE DATE_ADD(DATE(created_at), INTERVAL (
            CASE DAYOFWEEK(created_at)
                WHEN 1 THEN 5
                WHEN 7 THEN 6
                ELSE 7
            END
        ) DAY)
    END;
