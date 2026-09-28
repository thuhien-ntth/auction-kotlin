-- Đơn vị tiền tệ của sản phẩm do người bán chọn khi đăng (VND / USD / EUR).
-- Sản phẩm cũ mặc định VND. Giá khởi điểm và mọi bid của sản phẩm đều tính theo đơn vị này (không quy đổi).
ALTER TABLE products ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND';
