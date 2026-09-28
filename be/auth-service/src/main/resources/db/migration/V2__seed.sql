-- Tài khoản demo — dùng cho chấm bài/test, KHÔNG có màn Register trong scope nên seed trực tiếp thay vì qua API.
-- password cho cả 3 tài khoản: "Passw0rd!" (bcrypt cost=10, hash thật, đã verify bằng python bcrypt lib)
INSERT INTO users (email, password_hash, full_name, is_admin) VALUES
  ('admin@auction.local',  '$2a$10$R/ItNCB1YICxXdtdwtfW6uzEKIdHlcBvdl1h/1Dtvgt2aiEHXh.Rm', 'System Admin', TRUE),
  ('seller1@auction.local','$2a$10$R/ItNCB1YICxXdtdwtfW6uzEKIdHlcBvdl1h/1Dtvgt2aiEHXh.Rm', 'Seller One',   FALSE),
  ('bidder1@auction.local','$2a$10$R/ItNCB1YICxXdtdwtfW6uzEKIdHlcBvdl1h/1Dtvgt2aiEHXh.Rm', 'Bidder One',   FALSE);
