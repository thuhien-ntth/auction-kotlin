-- Truy vấn tìm kiếm của ProductRepository.search dùng LOWER(title) LIKE LOWER('%kw%').
-- Index cũ idx_products_title_trgm là gin(title gin_trgm_ops) — index trên title, không phải LOWER(title),
-- nên PostgreSQL không dùng được cho biểu thức LOWER(...). Đo trên 200.000 sản phẩm: truy vấn trang đầu
-- với từ khóa hiếm giảm từ ~89 ms xuống ~0,2 ms sau khi có index biểu thức (xem báo cáo mục 6.7).
CREATE INDEX IF NOT EXISTS idx_products_title_lower_trgm ON products USING gin (LOWER(title) gin_trgm_ops);

-- Index cũ không còn truy vấn nào dùng tới, bỏ đi để giảm chi phí ghi.
DROP INDEX IF EXISTS idx_products_title_trgm;
