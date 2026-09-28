# Kịch bản video demo ứng dụng đấu giá (5 phút, chú thích chữ) — bản 2, đã bỏ Event

**Phạm vi:** chỉ quay màn hình **app Android (fe/)**, app gọi API thật của toàn bộ backend qua Gateway. Không thuyết minh tiếng, chỉ có phụ đề chữ tiếng Việt dưới màn hình. Đây là kịch bản để bạn duyệt; **chưa quay gì**.

**Thay đổi so với bản 1:** không còn Event. Người bán đặt giờ bắt đầu/kết thúc ngay khi đăng sản phẩm; đến giờ hệ thống **tự mở** đấu giá, hết giờ **tự đóng**. Nhờ vậy bỏ được cảnh "tạo phiên" và cảnh "gắn sản phẩm vào phiên" (bản 1 phải làm bằng `curl` ngoài màn hình), toàn bộ demo diễn ra trong app.

> Đối chiếu với mã nguồn `fe/` và với lần chạy backend end-to-end trên mã mới (`evidence/e2e_log.json`, 54 lời gọi, 0 lỗi 5xx). **App Android chưa được biên dịch hay chạy trên emulator** (môi trường của tôi không có Android SDK), nên phần sửa FE mới chỉ kiểm tra bằng đọc mã; cần bạn build và chạy thử một lượt trước khi quay.

---

## 0. Những điều cần lưu ý trước khi quay

1. **Nút "Từ chối" đã được sửa** (mở hộp thoại nhập lý do, gửi `reason` đúng như backend yêu cầu), nên kịch bản có thể demo cả nhánh từ chối. Cần build lại app để có thay đổi này; chưa được biên dịch thử trong môi trường của tôi.
2. **URL backend cấu hình được** (không cần sửa mã): thêm dòng `api.base.url=http://10.0.2.2:8080/api/` vào `fe/local.properties` (emulator) hoặc `api.base.url=http://<IP LAN máy chạy backend>:8080/api/` (điện thoại thật), hoặc build với `-PapiBaseUrl=...`. Mặc định vẫn là `http://192.168.0.101:8080/api/`. Phải kết thúc bằng `/api/`.
3. **Giờ đấu giá nhập tay, định dạng UTC ISO 8601** (ví dụ `2026-09-21T10:30:00Z`), múi giờ Việt Nam trừ 7 giờ. Nhập tay trên điện thoại rất chậm và dễ sai, nên chuẩn bị sẵn ba dòng giờ trong ghi chú để dán (xem mục 1).
4. **Độ trễ mở/đóng tối đa ~5 giây** (job chạy mỗi 5 giây). Sản phẩm vừa đến giờ sẽ xuất hiện sau tối đa 5 giây.
5. **OTP đăng ký** đọc từ **MailHog** (`http://localhost:8025`, có trong Docker Compose), cần cắt sang trình duyệt 5–8 giây.

---

## 1. Chuẩn bị (không quay)

| Việc | Chi tiết |
|---|---|
| Chạy backend | `cd be && docker compose up --build` (cần `.env`, xem `.env.example`) |
| Tài khoản có sẵn (mật khẩu `Passw0rd!`) | `admin@auction.local` (Admin), `seller1@auction.local` (người bán), `bidder1@auction.local` (người mua 1) |
| Tài khoản tạo lúc quay | Đăng ký mới làm **người mua 2**, ví dụ `bidder2@demo.local` |
| Ảnh sản phẩm | Chép 2–3 ảnh (laptop, đồng hồ, điện thoại) vào thư viện ảnh của emulator |
| Ba mốc giờ cho ba sản phẩm | Gọi thời điểm bắt đầu ghi cảnh 4 là **T** (UTC). **A**: bắt đầu = T − 1 phút, kết thúc = T + 60 phút (đang đấu giá lâu). **B**: bắt đầu = T − 1 phút, kết thúc = **T + 4 phút** (để xem tự đóng). **C**: bắt đầu = **T + 2 phút 30 giây**, kết thúc = T + 60 phút (để xem tự mở). Tính sẵn ba cặp giờ ngay trước khi quay và lưu vào ghi chú |
| Thiết bị quay | 1 emulator, quay bằng công cụ của Android Studio hoặc OBS. Đổi vai trò bằng đăng xuất/đăng nhập |
| Cách quay | Quay liền một mạch từ cảnh 4 đến cảnh 9 để các mốc T + 2:30 và T + 4:00 rơi đúng chỗ; các cảnh 1–3 và 10–11 quay riêng rồi ghép. Mốc giờ của B và C chỉ là ước lượng: quay thử một lần để đo thời gian thật từ cảnh 4 đến cảnh 9 rồi chỉnh lại |

---

## 2. Bảng cảnh (tổng ~5:00)

Ký hiệu **[API]** là lời gọi backend thật mà cảnh đó kích hoạt (qua Gateway `/api/...`).

| # | Thời gian (video) | Cảnh | Thao tác trên app | Caption | [API] và kết quả kỳ vọng |
|---|---|---|---|---|---|
| 1 | 0:00–0:15 | Mở đầu | Thẻ tiêu đề, rồi hình kiến trúc (Gateway + 3 service) | "Ứng dụng đấu giá trực tuyến: Android + Gateway + Auth / Catalog / Bidding (Kotlin, Spring Boot)" | — |
| 2 | 0:15–0:50 | Đăng ký và xác thực email | Màn **Register** nhập email, mật khẩu, họ tên. Sang **Verify Email**. Cắt sang MailHog đọc OTP, nhập vào app. Có thể nhập sai mã 1 lần | "Đăng ký tài khoản, mã xác thực gửi qua email" | `POST /auth/register` → 200; `POST /auth/verify` → 200 (mã sai → 400 "Invalid verification code") |
| 3 | 0:50–1:05 | Đăng nhập người bán | Màn **Login**: `seller1@auction.local` | "Đăng nhập bằng JWT" | `POST /auth/login` → 200 |
| 4 | 1:05–1:55 | Người bán đăng 3 sản phẩm kèm giờ | Tab **Create product**: nhập tên, giá, chọn ảnh, **giờ bắt đầu/kết thúc** theo bảng ở mục 1. Sản phẩm **A** "Laptop Demo A" giá 100; **B** "Đồng hồ Demo B" giá 50; **C** "Điện thoại Demo C" giá 80. Sang tab **My product** xem ba sản phẩm chờ duyệt | "Người bán tự đặt thời gian đấu giá; trạng thái chờ duyệt: PENDING_APPROVAL" | `POST /products` → 201 (ba lần); `POST /products/{id}/image` → 200; `GET /my/products` → 200. Nếu bỏ trống giờ: nút gửi bị vô hiệu; giờ kết thúc ≤ giờ bắt đầu hoặc ở quá khứ → 400 |
| 5 | 1:55–2:20 | Admin duyệt | Đăng xuất, đăng nhập `admin@auction.local`. Tab **Admin**: bấm **Duyệt SP** cho A, B, C | "Admin duyệt sản phẩm; hệ thống tự mở đấu giá khi đến giờ" | `GET /admin/products/pending` → 200; `POST /admin/products/{id}/approve` → 200 (APPROVED). Duyệt sản phẩm đã quá giờ kết thúc → 409 |
| 6 | 2:20–2:45 | Người mua tìm kiếm, xem sản phẩm | Đăng nhập `bidder1@auction.local`. Tab **Search**: gõ "Demo": thấy **A và B** (đang đấu giá), **chưa thấy C** vì chưa đến giờ. Mở chi tiết A | "Chỉ sản phẩm đang trong thời gian đấu giá mới hiện; giá hiện tại lấy từ bidding-service" | `GET /products?keyword=Demo` → 200 (2 kết quả); `GET /products/{id}` → 200 (status ACTIVE) |
| 7 | 2:45–3:45 | Đặt giá | Trên chi tiết A (giá 100): a) bidder1 đặt 120 → thành công, thanh dưới hiện "đang dẫn giá"; b) bidder1 đặt tiếp → bị chặn "You are already the highest bidder…"; c) đăng xuất, đăng nhập người mua 2 (tài khoản mới): đặt giá thấp hơn 120 → lỗi "must be higher than the current price"; d) đặt 150 → thành công. Trên B, bidder1 đặt 60 (để B có người thắng). Nếu app cho hiện nút, cho seller1 thử đặt giá vào A → lỗi | "Đặt giá: kiểm tra giá tối thiểu, chống đặt liên tiếp, người bán không tự đặt giá" | `POST /products/{id}/bids` → 200 / 409 (giá thấp, liên tiếp, người bán) / 401 (chưa đăng nhập); `GET /products/{id}/bids` (lịch sử, bid bị từ chối vẫn lưu với `accepted=false`) |
| 8 | 3:45–4:05 | Sản phẩm C tự mở | Làm mới tab **Search**: **C xuất hiện** (đã đến T + 2:30). Mở C, đặt giá 100 → thành công. Tab **Join** của bidder1 cho thấy A, B đang tham gia | "Đến giờ bắt đầu, sản phẩm tự chuyển sang ACTIVE, không cần thao tác nào" | Job `AuctionScheduler` chuyển APPROVED → ACTIVE (log "Mở đấu giá cho 1 sản phẩm"); `GET /my/bids/participating` → 200 |
| 9 | 4:05–4:35 | B tự đóng, xem sản phẩm đã thắng | Đến T + 4:00, B hết giờ. Làm mới: B **biến mất khỏi tìm kiếm**. Đăng nhập bidder1, tab **Won**: thấy B, giá 60. Thử đặt giá vào B → lỗi "The auction has ended" | "Hết giờ, hệ thống tự đóng phiên và xác định người thắng" | Job đóng B → SOLD (log "Đóng phiên đấu giá … -> SOLD"); `GET /my/bids/won` → 200 (B, 60); bid sau khi hết giờ → 409 |
| 10 | 4:35–4:50 | Đăng xuất | Bấm **Exit** ở thanh dưới; thử dùng lại phiên cũ | "Đăng xuất: token vào blacklist Redis, dùng lại bị từ chối (401)" | `POST /auth/logout` → 204; token cũ → 401 "Token has been logged out" |
| 11 | 4:50–5:00 | Kết luận | Thẻ tổng kết số liệu (0 lỗi 5xx trong 54 lời gọi end-to-end; đúng 100% khi 50–500 người cùng đặt giá) | "Đặt giá đồng thời đúng 100%; tự mở/đóng phiên theo giờ" | — |

**Sắp xếp thời gian cho cảnh 8–9:** giờ của B (kết thúc) và C (bắt đầu) được tính theo lúc bạn bấm tạo sản phẩm ở cảnh 4. Cần chọn sao cho C tự mở đúng lúc quay cảnh 8 và B tự đóng đúng lúc quay cảnh 9; vì vậy nên quay thử một lần để biết cảnh 4–7 kéo dài bao lâu rồi mới đặt mốc giờ chính thức.

---

## 3. Những gì kịch bản KHÔNG cover (nói thẳng để không nói quá trong báo cáo)

- **Từ chối sản phẩm:** có thể thêm một cảnh ngắn: admin bấm "Từ chối", nhập lý do, seller thấy "Lý do từ chối" ở chi tiết sản phẩm (cần build lại app với bản sửa).
- **Gửi lại mã xác thực (`resend-verification`) và xem hồ sơ (`/users/me`):** app chưa gọi hai API này.
- **Chịu lỗi khi tắt bidding-service:** không quay trong app; đã có bằng chứng ở báo cáo mục 6.12.1. Nếu muốn, thêm cảnh 15 giây: tắt bidding-service, trang chi tiết vẫn mở được nhưng hiện giá khởi điểm, đặt giá báo lỗi.
- **Sửa giờ đấu giá sau khi đăng:** không có chức năng này.
- **App Android chưa được biên dịch/chạy trong phiên làm việc này;** mọi mô tả thao tác dựa trên mã nguồn và cần thử một lượt trước khi quay chính thức.

---

## 4. Câu hỏi để bạn duyệt

1. Đồng ý cấu trúc ba sản phẩm A / B / C (đang đấu giá lâu / tự đóng / tự mở) không?
2. ~~Sửa nút "Từ chối"~~ đã làm xong (xem mục 0.1).
3. Nhập giờ tay khá chậm: có muốn thêm **ô chọn giờ nhanh** (ví dụ nút "+1 phút", "+5 phút") vào màn đăng sản phẩm để quay đỡ vướng không?
4. Chạy trên emulator hay điện thoại thật? (Chỉ ảnh hưởng dòng `api.base.url` trong `local.properties`.)
5. Có cần thêm cảnh tắt bidding-service (khoảng 15 giây) không?
