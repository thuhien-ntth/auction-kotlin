-- Danh mục: thêm "Thời trang" + cột sort_order để tự đặt thứ tự hiển thị (trước đây sắp theo name ASC).

INSERT INTO categories (id, name, created_at)
VALUES (gen_random_uuid(), 'Thời trang', NOW())
ON CONFLICT (name) DO NOTHING;

-- Danh mục thêm mới sau này mặc định 1000 -> nằm cuối danh sách.
ALTER TABLE categories ADD COLUMN sort_order INT NOT NULL DEFAULT 1000;

-- Giữ nguyên thứ tự hiện tại (theo tên) cho các danh mục đang có, cách nhau 10 để dễ chèn vào giữa.
UPDATE categories c
SET sort_order = s.rn * 10
FROM (
    SELECT id, ROW_NUMBER() OVER (ORDER BY name) AS rn
    FROM categories
    WHERE name <> 'Thời trang'
) s
WHERE c.id = s.id;

-- "Thời trang" đứng ngay sau "Trang sức" (nếu chưa có "Trang sức" thì xếp cuối).
UPDATE categories
SET sort_order = COALESCE(
        (SELECT sort_order FROM categories WHERE name = 'Trang sức'),
        (SELECT MAX(sort_order) FROM categories WHERE name <> 'Thời trang')
    ) + 5
WHERE name = 'Thời trang';
