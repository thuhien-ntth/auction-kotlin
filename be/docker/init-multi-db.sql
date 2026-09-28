-- Chạy đúng 1 lần khi volume pg-data được khởi tạo lần đầu (data dir rỗng).
-- POSTGRES_DB=auth_db đã được tạo sẵn bởi image entrypoint; ở đây chỉ tạo thêm
-- 2 database còn lại để catalog-service và bidding-service dùng chung 1 Postgres server.
CREATE DATABASE catalog_db OWNER auction;
CREATE DATABASE bidding_db OWNER auction;
