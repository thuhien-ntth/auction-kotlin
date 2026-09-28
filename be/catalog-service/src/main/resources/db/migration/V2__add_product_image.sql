-- Thêm field ảnh sản phẩm (tính năng upload ảnh khi tạo sản phẩm — bổ sung sau bản thiết kế gốc,
-- xem ARCHITECTURE_DESIGN.md mục 1 và API_CONTRACT.md mục "Upload ảnh sản phẩm").
-- Chỉ lưu TÊN FILE (không lưu URL đầy đủ) — catalog-service tự map sang GET /products/{id}/image
-- khi trả response, giữ đúng nguyên tắc "service tự quyết định cách trình bày dữ liệu của mình".
ALTER TABLE products ADD COLUMN image_path VARCHAR(500) NULL;
