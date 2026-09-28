-- Bỏ khái niệm Event: thời gian đấu giá nằm trực tiếp trên sản phẩm.
ALTER TABLE products ADD COLUMN auction_start_at TIMESTAMPTZ NULL;
ALTER TABLE products ADD COLUMN auction_end_at   TIMESTAMPTZ NULL;

-- Dữ liệu cũ: lấy thời gian từ event mà sản phẩm đã gắn vào.
UPDATE products p SET auction_start_at = e.start_time, auction_end_at = e.end_time
FROM events e WHERE p.event_id = e.id;

-- Sản phẩm chưa từng gắn event: cho cửa sổ mặc định 7 ngày kể từ lúc tạo.
UPDATE products SET auction_start_at = created_at, auction_end_at = created_at + INTERVAL '7 days'
WHERE auction_start_at IS NULL;

-- Trạng thái ACTIVE cũ được giữ nguyên; AuctionScheduler sẽ đóng những sản phẩm đã quá giờ kết thúc.
ALTER TABLE products ALTER COLUMN auction_start_at SET NOT NULL;
ALTER TABLE products ALTER COLUMN auction_end_at   SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT chk_products_auction_window CHECK (auction_end_at > auction_start_at);

DROP INDEX IF EXISTS idx_products_event;
ALTER TABLE products DROP COLUMN event_id;
DROP TABLE events;

-- Phục vụ AuctionScheduler: quét theo trạng thái và mốc thời gian.
CREATE INDEX idx_products_status_start ON products(status, auction_start_at);
CREATE INDEX idx_products_status_end   ON products(status, auction_end_at);
