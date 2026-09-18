-- Chuyển giá về tour: mỗi tour chỉ có 1 giá duy nhất, không còn giá riêng theo lịch khởi hành.
ALTER TABLE tour ADD COLUMN price DECIMAL(12,2);

UPDATE tour t SET price = COALESCE(
    (SELECT MIN(ts.price) FROM tour_schedule ts WHERE ts.tour_id = t.tour_id), 0);

ALTER TABLE tour ALTER COLUMN price SET NOT NULL;
ALTER TABLE tour ADD CONSTRAINT chk_tour_price CHECK (price >= 0);

ALTER TABLE tour_schedule DROP CONSTRAINT chk_ts_price;
ALTER TABLE tour_schedule DROP COLUMN price;
