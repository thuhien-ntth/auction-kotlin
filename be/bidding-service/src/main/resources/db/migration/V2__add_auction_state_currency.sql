-- Đơn vị tiền tệ của sản phẩm (copy từ catalog lúc khởi tạo auction_state), để list Đang tham gia / Đã thắng hiển thị đúng.
ALTER TABLE auction_state ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND';
