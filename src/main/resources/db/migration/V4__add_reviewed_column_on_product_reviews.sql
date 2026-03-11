ALTER TABLE order_items
    ADD reviewed BOOLEAN;

UPDATE order_items
SET reviewed = false;

ALTER TABLE order_items
    ALTER COLUMN reviewed SET NOT NULL;