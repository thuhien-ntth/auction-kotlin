# Auction System — Backend (Kotlin Microservices)

Khung code hiện thực hoá thiết kế trong `../ARCHITECTURE_DESIGN.md`. Đọc file đó trước để hiểu lý do đằng sau các quyết định dưới đây — README này chỉ nói **cách chạy**, không lặp lại phần **tại sao**.

## ⚠️ Tình trạng đã kiểm tra

Toàn bộ mã nguồn được viết và rà soát thủ công (kiểu dữ liệu, import, luồng gọi cross-service) trong môi trường cloud sandbox của Claude — **sandbox này bị chặn egress tới Maven Central / Gradle Plugin Portal theo policy tổ chức** nên **chưa chạy được `./gradlew build` thật để xác nhận biên dịch** trong lúc tạo ra khung code này. Việc đầu tiên nên làm khi nhận code về máy: chạy `./gradlew build` (máy cá nhân có Internet bình thường sẽ tải được dependency) và sửa các lỗi biên dịch nhỏ nếu có (khả năng cao chỉ là sai version dependency, không phải sai logic).

## Cấu trúc

```
be/
  api-gateway/       # Spring Cloud Gateway — JWT verify + routing, port 8080
  auth-service/       # Login/Logout/Register/Verify, port 8081
  catalog-service/     # Product + Admin approve/reject + tự mở/đóng đấu giá theo giờ, port 8082
  bidding-service/     # Manual bidding, participating/won, port 8083
  docker-compose.yml   # 3 Postgres + Redis + 4 service
```

## Chạy thử (docker compose)

```bash
cd be
docker compose up --build
```

Sau khi lên xong (đợi log flyway migrate hết ~10-15s), toàn bộ API vào qua **Gateway `http://localhost:8080/api/...`**.

Tài khoản demo (seed sẵn, xem `auth-service/.../V2__seed.sql`), mật khẩu chung `Passw0rd!`:
- `admin@auction.local` — isAdmin=true
- `seller1@auction.local`
- `bidder1@auction.local`

## Luồng demo end-to-end (curl)

```bash
BASE=http://localhost:8080/api

# 1. Login admin
ADMIN_TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@auction.local","password":"Passw0rd!"}' | jq -r .accessToken)

# 2. Login seller
SELLER_TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"seller1@auction.local","password":"Passw0rd!"}' | jq -r .accessToken)

# 3. Seller đăng sản phẩm kèm thời gian đấu giá -> PENDING_APPROVAL
#    (đặt bắt đầu = 1 phút trước, kết thúc = 1 giờ sau; đổi sang giờ bạn muốn, định dạng UTC ISO 8601)
START=$(date -u -d '-1 minute' +%FT%TZ); END=$(date -u -d '+1 hour' +%FT%TZ)
PRODUCT_ID=$(curl -s -X POST $BASE/products -H "Authorization: Bearer $SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"title\":\"Máy tiện CNC cũ\",\"description\":\"Đã qua sử dụng, hoạt động tốt\",\"category\":\"MACHINERY\",\"startPrice\":1000000,\"auctionStartAt\":\"$START\",\"auctionEndAt\":\"$END\"}" \
  | jq -r .id)

# 4. Admin duyệt -> APPROVED. Đến auctionStartAt hệ thống TỰ chuyển sang ACTIVE (trong vòng ~5 giây),
#    đến auctionEndAt TỰ đóng (SOLD nếu có người thắng, ENDED_NO_BID nếu không). Không còn Event, không cần gắn thủ công.
curl -s -X POST $BASE/admin/products/$PRODUCT_ID/approve -H "Authorization: Bearer $ADMIN_TOKEN"
sleep 6   # đợi job tự mở

# 5. Login bidder, đặt giá
BIDDER_TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"bidder1@auction.local","password":"Passw0rd!"}' | jq -r .accessToken)

curl -s -X POST $BASE/products/$PRODUCT_ID/bids -H "Authorization: Bearer $BIDDER_TOKEN" \
  -H 'Content-Type: application/json' -d '{"amount":1100000}'

# 8. Xem product detail (currentPrice ghép từ bidding-service)
curl -s $BASE/products/$PRODUCT_ID | jq

# 9. Bidder xem participating/won
curl -s $BASE/my/bids/participating -H "Authorization: Bearer $BIDDER_TOKEN" | jq

# 10. Logout
curl -s -X POST $BASE/auth/logout -H "Authorization: Bearer $BIDDER_TOKEN" -w "%{http_code}\n"
```

## Ghi chú triển khai quan trọng (map với ARCHITECTURE_DESIGN.md)

- `bidding-service` là nguồn ghi duy nhất cho `currentPrice` (mục 3.2). `catalog-service` chỉ đọc lại qua `BiddingClient` (API composition, mục 4).
- Đặt giá đồng thời dùng **optimistic locking** (`AuctionState.version`) + retry — xem `BiddingService.placeBid` + `BidTransactionExecutor` (tách bean riêng để tránh lỗi self-invocation của Spring `@Transactional`).
- Logout dùng Redis blacklist theo `jti`, TTL tự hết hạn — chỉ `auth-service` và `api-gateway` kiểm tra (mục 4, hàng "Bảo mật giữa các service").
- Mỗi service có `JwtAuthFilter` riêng tự verify chữ ký JWT, KHÔNG chỉ tin header do Gateway forward — cố ý trùng lặp code thay vì dùng shared library (mục 4).
- Có đăng ký tài khoản: `POST /api/auth/register` -> `verify` (OTP gửi qua email, hết hạn sau 15 phút) -> `resend-verification`. Ngoài ra vẫn có 3 tài khoản demo seed sẵn bằng Flyway (`V2__seed.sql`, đã verify sẵn).
- Lỗi trả về luôn theo dạng `{"statusCode":..,"message":..}`. Gateway trả `503` khi service phía sau không kết nối được và `504` khi quá thời gian (thay vì 500); JSON sai định dạng trả `400`; upload ảnh > 8MB trả `413`.

## Việc CHƯA làm (đã cắt khỏi scope, xem lý do ở mục 1 tài liệu thiết kế)

Negotiation flow (hàng "held"), Bulk Auto Bidding, Box/Lot, CSV import, Invoice, Notification/Email, Watchlist, Related Link, KYC. Nếu muốn mở rộng thêm, nên bắt đầu từ các `ProductStatus`/`domain` hiện có thay vì viết lại từ đầu.

## Cập nhật tối ưu hệ thống (đợt cải thiện)

- **Cổng mạng**: mặc định chỉ gateway `8080` được publish; auth/catalog/bidding chỉ truy cập được trong mạng Docker. Postgres/Redis bind `127.0.0.1`. Để debug hoặc chạy JMeter kịch bản 4: `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d` (publish 8081–8083 trên localhost).
- **Bí mật**: sao chép `.env.example` thành `.env` và đặt `JWT_SECRET`, `INTERNAL_TOKEN`. Endpoint `/internal/**` yêu cầu header `X-Internal-Token`.
- **Đặt giá**: từ chối bid không hợp lệ trả 409 kèm `message`; quá tải (hết lần retry optimistic lock) trả 503; người đang dẫn giá không được bid liên tiếp. Bid bị từ chối vẫn được ghi (`accepted=false`) — tắt bằng `bidding.record-rejected-bids=false`.
- **Mở/đóng phiên tự động**: thời gian đấu giá (`auctionStartAt`, `auctionEndAt`) do người bán đặt trên sản phẩm, không còn khái niệm Event. `AuctionScheduler` chạy mỗi `auction-closing.interval-ms` (mặc định 5000 ms): mở (APPROVED→ACTIVE) khi đến giờ bắt đầu, đóng khi hết giờ. Migration `V4` chuyển dữ liệu cũ và xóa bảng `events`. Đóng phiên: `AuctionScheduler` xử lý theo lô (`auction-closing.batch-size`), 1 lời gọi `POST /internal/products/results` cho cả lô, cập nhật có điều kiện nên chạy được nhiều instance.
- **Quan sát**: Actuator `/actuator/metrics` (`bidding.bids`, `bidding.optimistic.conflicts`, HikariCP, Tomcat); header `X-Trace-Id` được chuyển tiếp giữa các service.
- **Tuning**: `DB_POOL_SIZE`, `TOMCAT_MAX_THREADS`; timeout gọi liên service qua `*.connect-timeout-ms` / `*.read-timeout-ms`.
- Mã đã được biên dịch, test và chạy thử end-to-end trong sandbox (JDK 21); cần chạy lại `./gradlew build test` trên JDK 17 trước khi nộp.
