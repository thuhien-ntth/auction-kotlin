# Tài liệu API cho FE — Auction System

> Ban đầu được rút từ interface `AuctionApi.kt` (Retrofit) + DTO trong `network/model/Models.kt` + business rule suy ra từ `MockAuctionApi.kt` (bản mock đã bị tắt). **Bản cập nhật này đã đối chiếu lại với code backend hiện tại** (`api-gateway`, `auth-service`, `catalog-service`, `bidding-service`): các mục về endpoint, DTO, mã lỗi, luồng trạng thái sản phẩm và luật đặt giá đều theo code backend. Các nhận định về hành vi phía Android app mà không kiểm chứng được từ backend được đánh dấu **"theo bản trước, chưa kiểm tra lại"**. Tài liệu này là **hợp đồng (contract)** giữa FE và `be/*-service` (qua `api-gateway`).

## 0. Thay đổi đã thực hiện trong code FE (bỏ hard-code)

- `AppContainer.kt`: `USE_MOCK_DATA` đổi từ `true` → **`false`**. App giờ luôn khởi tạo `api` bằng `ApiClient.create(tokenStore)` (Retrofit thật), không còn dùng `MockAuctionApi` (dữ liệu in-memory giả) trong runtime. File `MockAuctionApi.kt` vẫn được giữ lại (không xoá) để bật lại tạm thời (`USE_MOCK_DATA = true`) khi cần demo UI lúc chưa có backend chạy. *(theo bản trước, chưa kiểm tra lại — mock có thể chưa theo kịp việc bỏ Event và thay đổi bắt buộc `auctionStartAt/auctionEndAt`)*
- `ProductSearchScreen.kt`: 2 chỗ hard-code hiển thị phân trang giả (`totalPages = 2` và chữ `"/30 件"` viết chết trong `PaginationBar`) đã được thay bằng `totalElements`/`totalPages` lấy thật từ response `PageResponse` của `GET /products`.
- **Phân trang `GET /products` hoạt động thật**: `AuctionApi.searchProducts()` có `page`/`size` (0-based, theo chuẩn Spring Data `Pageable`), `ProductSearchScreen` gọi lại API mỗi khi bấm nút trang. **Lưu ý còn lại**: `PaginationBar` hiện chỉ vẽ cố định 2 nút số trang "1"/"2" (chưa vẽ động theo `totalPages` thật) — giới hạn hiển thị, không phải hard-code dữ liệu. *(theo bản trước, chưa kiểm tra lại)*
- Có tính năng **upload ảnh sản phẩm** khi đăng sản phẩm — xem mục 3.5b và mục 6 (kiến trúc).
- **`API_BASE_URL` nay cấu hình được** (không còn hard-code): đọc từ `local.properties` key `api.base.url`, hoặc gradle property `-PapiBaseUrl=...`; mặc định `http://192.168.0.101:8080/api/`. Khi chạy Android Emulator dùng `http://10.0.2.2:8080/api/`. Giá trị **bắt buộc kết thúc bằng `/api/`** (Retrofit ghép path tương đối như `auth/login`, `products/{id}/image` vào sau base URL). Chi tiết ở mục 1.
- **Màn admin duyệt sản phẩm**: app đã được sửa để khi từ chối sản phẩm sẽ hiện hộp thoại nhập lý do và gửi `{"reason": "..."}` lên `POST /admin/products/{id}/reject` (backend bắt buộc `reason` không rỗng, xem mục 3.9).

## 1. Thông tin chung

| | |
|---|---|
| Base URL | Cấu hình được (mục 0): `local.properties` → `api.base.url=...`, hoặc `./gradlew ... -PapiBaseUrl=...`. Mặc định `http://192.168.0.101:8080/api/`. **Emulator** dùng `http://10.0.2.2:8080/api/` (alias tới `localhost` máy host); thiết bị thật dùng IP LAN của máy chạy gateway. Phải kết thúc bằng `/api/`. Cổng `8080` là `api-gateway`; gateway bỏ tiền tố `/api` (`StripPrefix=1`) rồi chuyển tới service tương ứng |
| Auth | Mọi request tự động kèm header `Authorization: Bearer <accessToken>` (do `AuthInterceptor` gắn, token đọc từ DataStore sau khi login) — endpoint **không cần** token: `POST /auth/login`, `POST /auth/register`, `POST /auth/verify`, `POST /auth/resend-verification`, `GET /products`, `GET /products/{id}`, `GET /products/{id}/image`, `GET /products/{id}/bids`. Gateway chỉ chặn (401) khi header có nhưng token sai/hết hạn/đã logout; không có header thì để service phía sau quyết định (endpoint cần đăng nhập trả `401 "Login required"`) |
| Content-Type | `application/json` (Gson, field name JSON = tên field Kotlin, không có `@SerializedName`) — riêng 2 endpoint ảnh dùng `multipart/form-data` (upload) và ảnh nhị phân thô (get), xem mục 3.5b |
| ID | Mọi id (`userId`, `sellerId`, `bidderId`, `product.id`, `bid.id`) là **UUID dạng chuỗi**, ví dụ `"3f2b8c1e-5a47-4d0b-9c1e-8f6a2d7b1c34"` — không phải dạng `"user-bidder-01"`/`"prod-1"` của dữ liệu mock cũ (các ví dụ JSON bên dưới dùng UUID rút gọn cho dễ đọc) |
| Số (giá tiền) | `BigDecimal` phía Kotlin → serialize thành **số JSON thuần** (không phải chuỗi), ví dụ `192000000` chứ không phải `"192000000"` |
| Thời gian | `createdAt`, `auctionStartAt`, `auctionEndAt` ở mọi DTO dùng chuỗi ISO-8601 UTC, ví dụ `"2026-09-14T10:12:00Z"` (FE parse bằng `Instant.parse`) |
| Phân trang | Mọi endpoint trả danh sách dùng `Pageable` của Spring Data: `page` (**0-based**) và `size`; response là `Page` với `content`, `totalElements`, `totalPages`, `number`, ... |
| Định dạng lỗi | Mọi lỗi từ gateway và cả 3 service đều có dạng `{"statusCode": <int>, "message": "..."}` — xem mục "Mã lỗi chung" bên dưới |

### Mã lỗi chung

Body lỗi thống nhất (đã đối chiếu `GlobalExceptionHandler` của `auth-service`, `catalog-service`, `bidding-service`, `JwtGatewayFilter`, `ApiErrorAttributes` và `DownstreamUnavailableHandler` của `api-gateway`):

```json
{ "statusCode": 409, "message": "The auction has not started yet" }
```

| HTTP | Khi nào | Ví dụ `message` |
|---|---|---|
| `400` | Validation lỗi (`@Valid`): `message` là danh sách `field: lý do` nối bằng `; `; hoặc lỗi nghiệp vụ dạng "dữ liệu sai" | `auctionEndAt: must not be null`, `auctionEndAt must be after auctionStartAt` |
| `401` | Thiếu/sai/hết hạn token (gateway hoặc service) hoặc login sai | `Invalid or expired token`, `Token has been logged out`, `Login required`, `Invalid email or password` |
| `403` | Không đủ quyền | `Admin privileges required`, `This is not your product`, `Account email is not verified yet. Please check your email.` |
| `404` | Không tìm thấy | `Product not found` |
| `409` | Xung đột trạng thái | `The auction has not started yet`, `Product is not in pending approval status` |
| `500` | Lỗi không lường trước | `Internal server error` |
| `503` | **Gateway**: service phía sau không kết nối được (connection refused) | `The service is temporarily unavailable, please try again` |
| `503` | **bidding-service**: quá nhiều xung đột đặt giá đồng thời (hết lượt retry) hoặc không gọi được catalog-service khi khởi tạo phiên | `The auction is very busy right now, please try again`, `Catalog service is temporarily unavailable` |
| `504` | **Gateway**: service phía sau không phản hồi kịp (timeout) | `The service did not respond in time, please try again` |

Lỗi phát sinh ngay tại Gateway mà không phải 401/503/504 (ví dụ không khớp route) cũng được chuẩn hoá về cùng dạng `{statusCode, message}`.

**Suy giảm có kiểm soát (graceful degradation)**: nếu `bidding-service` chết, `GET /products/{id}` **vẫn trả `200`** nhưng `currentPrice` rơi về `startPrice` (fallback trong `BiddingClient` của catalog-service; kết quả fallback không được cache). Các endpoint của chính bidding-service (`/products/{id}/bids`, `/my/bids/*`) thì trả `503` qua gateway.

## 2. Danh sách endpoint

| Method | Path | Dùng ở màn hình | Ghi chú |
|---|---|---|---|
| POST | `auth/login` | LoginScreen | Không cần token |
| POST | `auth/register` | Đăng ký | Không cần token, xem 3.1b |
| POST | `auth/verify` | Xác thực email | Không cần token, xem 3.1c |
| POST | `auth/resend-verification` | Xác thực email | Không cần token, xem 3.1d |
| POST | `auth/logout` | (chưa có nút gọi trong UI hiện tại) | Trả `204` |
| GET | `users/me` | Hồ sơ người dùng | Cần token, xem 3.2b |
| GET | `products?keyword=&category=&page=&size=` | ProductSearchScreen | Chỉ trả sản phẩm `ACTIVE`; `page`/`size` optional, 0-based, FE mặc định `size=10` |
| GET | `products/{id}` | ProductDetailScreen | Trả kèm `currentPrice` mới nhất (API composition với bidding) và `imageUrl` |
| POST | `products` | ProductRegisterScreen | Luôn tạo với `status = PENDING_APPROVAL`, trả `201`, `auctionStartAt`/`auctionEndAt` **bắt buộc**, thuần JSON, **không** nhận ảnh trong request này |
| **POST** | **`products/{id}/image`** | **ProductRegisterScreen (sau khi tạo xong), có thể tái dùng để đổi ảnh sau này** | Multipart — xem mục 3.5b |
| **GET** | **`products/{id}/image`** | **ProductThumbnail (mọi màn có hiển thị ảnh sản phẩm)** | Public, trả file ảnh nhị phân — xem mục 3.5b |
| GET | `my/products?status=` | ExhibitionManagementScreen | `status` optional, lọc theo trạng thái sản phẩm của user đang đăng nhập |
| GET | `admin/products/pending` | Màn admin duyệt sản phẩm | Chỉ admin, xem 3.9 |
| POST | `admin/products/{id}/approve` | Màn admin duyệt sản phẩm | Chỉ admin, xem 3.9 |
| POST | `admin/products/{id}/reject` | Màn admin duyệt sản phẩm | Chỉ admin, body `{"reason": "..."}`, xem 3.9 |
| **POST** | **`products/{id}/bids`** | **ProductDetailScreen — Manual Bidding** | Xem chi tiết mục 4 |
| GET | `products/{id}/bids` | ProductDetailScreen | Lịch sử đặt giá, mới nhất trước |
| GET | `my/bids/participating` | ParticipatingScreen | |
| GET | `my/bids/won` | WonScreen | |

## 3. Chi tiết request/response từng endpoint

### 3.1 `POST auth/login`
Request:
```json
{ "email": "bidder1@auction.local", "password": "Passw0rd!" }
```
Response `200`:
```json
{
  "accessToken": "jwt-token...",
  "userId": "3f2b8c1e-5a47-4d0b-9c1e-8f6a2d7b1c34",
  "fullName": "Người đấu giá (Bidder 1)",
  "isAdmin": false
}
```
`userId` là UUID dạng chuỗi. Lỗi: `401 "Invalid email or password"` (sai email hoặc mật khẩu); `403 "Account email is not verified yet. Please check your email."` nếu tài khoản đăng ký chưa xác thực email (xem 3.1b–3.1d); `400` nếu `email` không đúng định dạng hoặc rỗng.

### 3.1b `POST auth/register`
Request:
```json
{ "email": "new@auction.local", "password": "Passw0rd1", "fullName": "Nguyễn Văn A" }
```
Mật khẩu tối thiểu 8 ký tự và phải có cả chữ lẫn số. Response `200`:
```json
{ "message": "Registration successful. Please check your email for the verification code." }
```
Backend sinh mã xác thực 6 ký tự (chữ in hoa + số), hiệu lực **15 phút**, gửi qua email. Tài khoản chưa xác thực không đăng nhập được. Lỗi: `400` (validation / mật khẩu không đủ mạnh), `409 "Email is already in use"`.

### 3.1c `POST auth/verify`
Request: `{ "email": "new@auction.local", "token": "A1B2C3" }`. Response `200`, body rỗng. Lỗi: `404 "No user found with this email"`, `400 "Account has already been verified"`, `400 "Invalid verification code"`, `400 "Verification code has expired, please request a new one"`.

### 3.1d `POST auth/resend-verification`
Request: `{ "email": "new@auction.local" }`. Sinh mã mới (hiệu lực 15 phút), gửi lại email. Response `200`: `{ "message": "Verification code resent. Please check your email." }`. Lỗi: `404 "No user found with this email"`, `400 "Account has already been verified"`.

### 3.2 `POST auth/logout`
Không có body, cần token. Trả `204 No Content` (token bị đưa vào blacklist tới lúc hết hạn; các request sau với token đó nhận `401 "Token has been logged out"`). Lỗi `401 "Invalid token"` nếu không đọc được token.

### 3.2b `GET users/me`
Cần token. Response `200` (không có `accessToken`):
```json
{
  "id": "3f2b8c1e-5a47-4d0b-9c1e-8f6a2d7b1c34",
  "email": "bidder1@auction.local",
  "fullName": "Người đấu giá (Bidder 1)",
  "isAdmin": false
}
```
Lỗi: `401` nếu thiếu/sai token, `404 "User not found"`. Lưu ý field id ở đây tên là `id`, khác `userId` của response login.

### 3.3 `GET products?keyword={kw}&category={cat}&page={page}&size={size}`
`keyword`, `category`, `page`, `size` đều optional. Param tìm kiếm tên là **`keyword`** (khớp không phân biệt hoa/thường, dạng "chứa" trên `title`; chuỗi rỗng/toàn khoảng trắng coi như không lọc); `category` khớp chính xác. `page` **0-based**, `size` do client chọn. Không có param `status` riêng: **endpoint này chỉ trả sản phẩm đang `ACTIVE`** (đang trong `[auctionStartAt, auctionEndAt)`). Sản phẩm `PENDING_APPROVAL`, `REJECTED`, `SOLD`, `ENDED_NO_BID` không xuất hiện, và một sản phẩm `APPROVED` mà `auctionStartAt` ở tương lai **chưa tìm được** ở đây — nó chỉ hiện ở `GET my/products` (của người bán) và chuyển thành `ACTIVE` (rồi mới searchable) khi đến giờ bắt đầu (xem mục 7). Xem sản phẩm theo id (`GET products/{id}`) thì không phụ thuộc trạng thái.

Response `200`:
```json
{
  "content": [
    {
      "id": "0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b",
      "title": "Máy ảnh Leica M11 Rangefinder Body Black",
      "category": "ELECTRONICS",
      "startPrice": 185000000,
      "status": "ACTIVE",
      "auctionStartAt": "2026-09-10T08:00:00Z",
      "auctionEndAt": "2026-09-25T21:00:00Z",
      "imageUrl": "products/0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b/image"
    }
  ],
  "totalElements": 4,
  "totalPages": 1,
  "number": 0
}
```
`auctionStartAt`/`auctionEndAt` luôn **không null** (đã bắt buộc khi tạo sản phẩm). `imageUrl` là **đường dẫn tương đối** (không phải URL đầy đủ), `null` nếu sản phẩm chưa có ảnh. FE tự ghép `BuildConfig.API_BASE_URL + imageUrl` để ra URL đầy đủ nạp vào Coil (`ProductThumbnail.kt`, `NjAuctionItemCard.kt`) — xem mục 3.5b và mục 6.

### 3.4 `GET products/{id}`
Response `200` — `ProductDetail` (thêm `sellerId`, `description`, `currentPrice`, `rejectionReason`, `createdAt` so với `ProductSummary`):
```json
{
  "id": "0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b",
  "sellerId": "9a8b7c6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
  "title": "Máy ảnh Leica M11 Rangefinder Body Black",
  "description": "Hàng chính hãng fullbox 99%...",
  "category": "ELECTRONICS",
  "startPrice": 185000000,
  "auctionStartAt": "2026-09-10T08:00:00Z",
  "auctionEndAt": "2026-09-25T21:00:00Z",
  "currentPrice": 192000000,
  "status": "ACTIVE",
  "rejectionReason": null,
  "imageUrl": "products/0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b/image",
  "createdAt": "2026-09-01T08:00:00Z"
}
```
`rejectionReason` chỉ khác `null` khi `status = REJECTED`. `currentPrice` = giá cao nhất hiện tại lấy từ bidding-service; chưa có bid nào → bằng `startPrice`; **bidding-service không phản hồi → vẫn `200` với `currentPrice = startPrice`** (fallback). `404 "Product not found"` nếu không tìm thấy sản phẩm.

### 3.5 `POST products`
Request:
```json
{
  "title": "Tên sản phẩm",
  "description": "Mô tả",
  "category": "ELECTRONICS",
  "startPrice": 5000000,
  "auctionStartAt": "2026-09-15T08:00:00Z",
  "auctionEndAt": "2026-09-20T20:00:00Z"
}
```
Cần token. Ràng buộc:
- `title` không rỗng; `startPrice` >= 0.01; `description` mặc định `""`, `category` mặc định `"OTHER"` nếu không gửi.
- **`auctionStartAt` và `auctionEndAt` BẮT BUỘC** (ISO-8601 UTC); thiếu → `400` (`auctionStartAt: must not be null`, ...).
- `auctionEndAt` phải sau `auctionStartAt`, nếu không → `400 "auctionEndAt must be after auctionStartAt"`.
- `auctionEndAt` phải ở tương lai, nếu không → `400 "auctionEndAt must be in the future"`.
- `auctionStartAt` **được phép ở quá khứ** (miễn `auctionEndAt` ở tương lai và sau start); khi đó sản phẩm mở ngay sau khi admin duyệt (scheduler chuyển `APPROVED -> ACTIVE` ở lượt kế tiếp).

Response `201` trả về `ProductDetail` vừa tạo với `status = "PENDING_APPROVAL"`, `sellerId` = user hiện tại (theo token), `currentPrice = startPrice`, `imageUrl = null` (ảnh luôn được upload ở bước sau qua endpoint riêng — xem 3.5b), `auctionStartAt`/`auctionEndAt` đúng như đã gửi. Sản phẩm chưa hiện ở `GET products` cho tới khi admin duyệt và đến giờ bắt đầu (mục 7).

### 3.5b `POST products/{id}/image` và `GET products/{id}/image` — Upload/hiển thị ảnh sản phẩm

**Vì sao tách endpoint riêng thay vì nhận ảnh luôn trong `POST /products`**: request tạo sản phẩm giữ nguyên `application/json` thuần (không đổi sang `multipart/form-data`), tránh phá vỡ toàn bộ 3.5 hiện có và giữ 2 trách nhiệm tách bạch — tạo bản ghi sản phẩm vs. lưu trữ file nhị phân. FE gọi tuần tự: `POST /products` lấy `id` → `POST /products/{id}/image` với `id` vừa tạo.

**`POST products/{id}/image`** — multipart/form-data, 1 field duy nhất tên `file`:
```
Content-Type: multipart/form-data; boundary=...

--boundary
Content-Disposition: form-data; name="file"; filename="product.jpg"
Content-Type: image/jpeg

<bytes>
--boundary--
```
- Yêu cầu JWT hợp lệ; **chỉ seller sở hữu sản phẩm** (`product.sellerId == token.userId`) mới được upload — người khác nhận `403 "This is not your product"`; sản phẩm không tồn tại → `404 "Product not found"`.
- Chỉ chấp nhận `image/jpeg`, `image/png`, `image/webp` (kiểm tra `Content-Type` của phần multipart, không dựa vào đuôi file) — sai định dạng trả `400 "Only JPEG/PNG/WebP images are supported (received: ...)"`; file rỗng trả `400 "Image file is empty"`.
- Giới hạn kích thước file: `8MB` (`spring.servlet.multipart.max-file-size` và `max-request-size` đều `8MB`). Vượt quá: **chưa kiểm tra lại được mã trả về thực tế** — trong code catalog-service không có handler riêng cho lỗi vượt dung lượng, nên nhiều khả năng rơi vào handler chung (`500 "Internal server error"`) thay vì `413` như bản trước ghi; FE nên tự chặn file > 8MB trước khi upload.
- Không giới hạn theo `status`: seller có thể đổi ảnh bất cứ lúc nào, kể cả sau khi `ACTIVE`.
- Gọi lại nhiều lần với cùng `productId` sẽ **ghi đè ảnh cũ** (không tạo bản ghi ảnh mới, không giữ lịch sử ảnh — ngoài phạm vi đồ án).
- Response `200` — trả về `ProductDetail` đã cập nhật (`imageUrl` giờ khác `null`), để FE có thể refresh UI ngay mà không cần gọi thêm `GET /products/{id}`.

**`GET products/{id}/image`** — **public, không cần JWT** (giống triết lý `GET /products/{id}` — xem ảnh sản phẩm không phải thông tin nhạy cảm, và Coil/trình duyệt cần load ảnh trực tiếp bằng URL mà không tự đính kèm header `Authorization`):
- Response `200`: body là bytes ảnh thô, header `Content-Type` khớp định dạng đã lưu (`image/jpeg`/`image/png`/`image/webp`).
- `404 Not Found` nếu sản phẩm không tồn tại (`"Product not found"`) **hoặc** chưa từng upload ảnh (`"Product has no image yet"`) — FE xử lý bằng cách không gọi endpoint này khi `imageUrl == null` (Coil chỉ được yêu cầu load khi có `imageUrl`, xem `ProductThumbnail.kt`), nên trong luồng bình thường FE không bao giờ nhận `404` ở đây. *(phần mô tả hành vi FE: theo bản trước, chưa kiểm tra lại)*

### 3.6 `GET my/products?status={status}`
Cần token. `status` optional (một trong `PENDING_APPROVAL | APPROVED | REJECTED | ACTIVE | SOLD | ENDED_NO_BID`; giá trị lạ → `400`). Response `200` — `PageResponse<ProductSummary>` (cấu trúc như mục 3.3, có kèm `imageUrl`, `auctionStartAt`, `auctionEndAt`), trả sản phẩm mà `sellerId == user hiện tại`, không giới hạn trạng thái nếu `status` rỗng. Đây là nơi người bán thấy sản phẩm `PENDING_APPROVAL`, `APPROVED` (chờ giờ bắt đầu) và `REJECTED`.

### 3.7 `GET products/{id}/bids`
Response `200` — `PageResponse<BidResponse>`, sắp xếp **mới nhất trước** (FE hiển thị trực tiếp theo thứ tự trả về, không tự sort lại):
```json
{
  "content": [
    { "id": "5d4c3b2a-1f0e-4d9c-8b7a-6f5e4d3c2b1a", "productId": "0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b", "bidderId": "3f2b8c1e-5a47-4d0b-9c1e-8f6a2d7b1c34", "amount": 192000000, "accepted": true, "createdAt": "2026-09-12T15:30:00Z" },
    { "id": "7e6d5c4b-3a29-4180-9f7e-6d5c4b3a2918", "productId": "0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b", "bidderId": "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d", "amount": 188000000, "accepted": true, "createdAt": "2026-09-12T14:10:00Z" }
  ],
  "totalElements": 2, "totalPages": 1, "number": 0
}
```
Endpoint này public (không cần token). Lịch sử **bao gồm cả bid bị từ chối** (`accepted: false`, xem mục 4.2), vì backend lưu lại cả các lần đặt giá không hợp lệ.

Quan trọng: FE lấy **phần tử đầu tiên có `accepted == true`** trong list này để xác định "ai đang giữ giá cao nhất" (`history.firstOrNull { it.accepted }?.bidderId`) — nên response **bắt buộc phải sắp theo thời gian giảm dần** (backend đúng như vậy: `ORDER BY createdAt DESC`), nếu không banner "Bạn đang giữ giá cao nhất" sẽ hiển thị sai người. *(cách FE dùng list này: theo bản trước, chưa kiểm tra lại)*

### 3.8 `GET my/bids/participating` / `GET my/bids/won`
Cần token. Cả hai trả `PageResponse<AuctionStateResponse>`:
```json
{
  "content": [
    { "productId": "0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b", "currentPrice": 192000000, "currentBidderId": "3f2b8c1e-5a47-4d0b-9c1e-8f6a2d7b1c34", "auctionEndAt": "2026-09-25T21:00:00Z" }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0
}
```
`participating` = sản phẩm user đã từng đặt giá **hợp lệ (`accepted = true`)** và phiên **chưa kết thúc** (`auctionEndAt > now`); `won` = sản phẩm có phiên **đã kết thúc** (`auctionEndAt < now`) và user là `currentBidderId` (người giữ giá cao nhất cuối cùng). `currentBidderId` có thể `null` về mặt kiểu dữ liệu.

**Lưu ý về ảnh ở 2 màn này**: `AuctionStateResponse` **cố tình không có `imageUrl`** — xem giải thích kiến trúc ở mục 6.2 (ParticipatingScreen/WonScreen không gọi sang bidding-service để lấy lại field thuộc catalog-service).

### 3.9 Admin — duyệt sản phẩm (`/admin/**`)
Cả 3 endpoint yêu cầu token của tài khoản `isAdmin = true`; không phải admin → `403 "Admin privileges required"`; không có token → `401`. Gateway chuyển `/api/admin/**` tới `catalog-service`.

**`GET admin/products/pending?page=&size=`** — `PageResponse<ProductSummary>` gồm các sản phẩm `status = PENDING_APPROVAL` (cấu trúc như 3.3).

**`POST admin/products/{id}/approve`** — không có body. Chuyển `PENDING_APPROVAL -> APPROVED`, trả `ProductDetail` (`status = "APPROVED"`, `currentPrice = startPrice`). Sản phẩm **không** thành `ACTIVE` ngay: `AuctionScheduler` tự chuyển sang `ACTIVE` khi đến `auctionStartAt` (mục 7). Lỗi:
- `404 "Product not found"`.
- `409 "Product is not in pending approval status"` nếu sản phẩm không ở `PENDING_APPROVAL`.
- `409 "The auction end time has already passed, cannot approve"` nếu `auctionEndAt` đã qua (không còn duyệt được; admin nên từ chối).

**`POST admin/products/{id}/reject`** — body bắt buộc:
```json
{ "reason": "Ảnh mờ, mô tả không đủ thông tin" }
```
`reason` **không được rỗng/toàn khoảng trắng** (thiếu hoặc rỗng → `400`, `message` dạng `reason: must not be blank`). Chuyển `PENDING_APPROVAL -> REJECTED`, lưu `rejectionReason = reason`, trả `ProductDetail`. Lỗi `404`/`409 "Product is not in pending approval status"` như trên. Người bán thấy lý do qua `rejectionReason` ở `GET products/{id}`.

## 4. Manual Bidding — `POST products/{id}/bids` (trọng tâm)

### 4.1 Cơ chế thật trên UI (không phải nhập giá tự do)

`ProductDetailScreen` **không có ô nhập số tiền tự do**. Luồng thật *(mô tả UI: theo bản trước, chưa kiểm tra lại)*:

1. FE tự tính danh sách "bước nhảy" (Step Value) từ `currentPrice` bằng `StepValueCalculator` (thuần client-side, không gọi API) theo bảng mốc giá cố định:

   | Khoảng `currentPrice` (USD) | 4 step chuẩn |
   |---|---|
   | 0 – 100 | 5 / 10 / 15 / 20 |
   | 101 – 500 | 50 / 100 / 150 / 200 |
   | 501 – 3.000 | 500 / 1.000 / 1.500 / 2.500 |
   | 3.001 – 4.000 | 2.000 / 4.000 |
   | 0 – 10.000.000 (VND, khi `currentPrice` > 100.000) | 500.000 / 1.000.000 / 2.000.000 / 5.000.000 |
   | 10.000.001 – 100.000.000 | 2.000.000 / 5.000.000 / 10.000.000 / 20.000.000 |
   | 100.000.001 – 1.000.000.000 | 5.000.000 / 10.000.000 / 20.000.000 / 50.000.000 |

   Nếu `currentPrice` không chia hết cho step đầu tiên của khoảng (`S1`), toàn bộ 4 step bị trừ đi phần dư (`currentPrice % S1`) để giá đặt luôn là số "đẹp" (Temporary Step Value). Đối chiếu spec gốc (`USEDS0030000 Product Detail/Bidding Flow` mục 4.1): control "Step Value" trên màn hình chỉ hiển thị các mức bước nhảy dạng nút chọn (chip) do hệ thống tính sẵn — **không có ô nhập số tự do** cho step, khớp đúng cách FE đã implement.
2. User bấm chọn 1 chip step → `targetBidPrice = currentPrice + step` → hiện popup xác nhận → bấm "Xác nhận đặt giá" → gọi:
   ```json
   POST products/{id}/bids
   { "amount": <targetBidPrice> }
   ```
   → **backend không cần biết gì về khái niệm "step"**, chỉ nhận một con số `amount` (>= 0.01) cuối cùng và tự validate độc lập. Cần token.

### 4.2 Response thật của backend (đã đối chiếu `BiddingService` / `BidTransactionExecutor`)

**Hành vi thật khác với bản trước của tài liệu này**: backend **không** trả `200` với `accepted:false`. Chỉ có đúng một trường hợp `200`: bid được chấp nhận. Mọi bid không hợp lệ đều bị trả về lỗi HTTP **`409 Conflict`** với body `{"statusCode":409,"message":"..."}`; đồng thời một bản ghi `Bid` với `accepted = false` **vẫn được lưu** (cấu hình `bidding.record-rejected-bids`, mặc định `true`) nên xuất hiện trong `GET products/{id}/bids` với `accepted: false`. Vì vậy trong response của `POST /bids`, `accepted` luôn là `true` (FE không bao giờ nhận `accepted:false` từ endpoint này, chỉ thấy trong lịch sử).

```json
// 200 OK — bid được chấp nhận (trở thành giá cao nhất mới)
{ "id": "5d4c3b2a-1f0e-4d9c-8b7a-6f5e4d3c2b1a", "productId": "0b6e4f7a-1c2d-4e5f-8a9b-0c1d2e3f4a5b", "bidderId": "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d", "amount": 197000000, "accepted": true, "createdAt": "2026-09-14T10:12:00Z" }

// 409 Conflict — bid bị từ chối
{ "statusCode": 409, "message": "Bid amount must be higher than the current price (192000000.00)" }
```

Bảng các tình huống (thứ tự kiểm tra trong code là thứ tự từ trên xuống ở nhóm "phiên đang có"; nếu vi phạm nhiều điều kiện cùng lúc, thông báo của điều kiện đứng trước được trả):

| Tình huống | HTTP | `message` | Lưu Bid `accepted=false`? |
|---|---|---|---|
| `amount` thiếu hoặc < 0.01 | `400` | `amount: ...` (validation) | Không |
| Chưa đăng nhập | `401` | `Login required` | Không |
| Sản phẩm không tồn tại (lần bid đầu tiên trên sản phẩm) | `404` | `Product not found` | Không |
| Sản phẩm chưa có trong hệ thống đấu giá sau khi khởi tạo (hiếm) | `404` | `Product not yet in the auction system` | Không |
| Lần bid đầu: sản phẩm chưa `ACTIVE` (còn `PENDING_APPROVAL`/`APPROVED`/`REJECTED`, hoặc đã `SOLD`/`ENDED_NO_BID`) | `409` | `The auction is not open for bidding` | Không |
| Lần bid đầu: `now < auctionStartAt` | `409` | `The auction has not started yet` | Không |
| Phiên đã kết thúc (`now >= auctionEndAt`) | `409` | `The auction has ended` | Có |
| Người đặt giá chính là người bán sản phẩm | `409` | `Sellers cannot bid on their own product` | Có |
| Người đặt giá đang là người giữ giá cao nhất | `409` | `You are already the highest bidder; wait for another bid before bidding again` | Có |
| `amount` <= giá hiện tại | `409` | `Bid amount must be higher than the current price (<currentPrice>)` | Có |
| Quá nhiều bid đồng thời trên cùng sản phẩm, hết lượt retry (mặc định 8) | `503` | `The auction is very busy right now, please try again` | Không |
| Không gọi được catalog-service khi khởi tạo phiên (lần bid đầu) | `503` | `Catalog service is temporarily unavailable` | Không |
| bidding-service chết / không kết nối được (qua gateway) | `503` | `The service is temporarily unavailable, please try again` | Không |

Ghi chú:
- Các kiểm tra "chưa `ACTIVE`" và "chưa bắt đầu" chỉ chạy khi khởi tạo trạng thái đấu giá của sản phẩm (lần bid đầu tiên). Sau khi đã có trạng thái, bidding-service chỉ kiểm tra thời gian kết thúc (do `AuctionScheduler` của catalog-service mới chuyển `status` sang `SOLD`/`ENDED_NO_BID`, còn bid bị chặn ngay khi `now >= auctionEndAt` mà không phụ thuộc scheduler).
- **Quy tắc "người đang giữ giá cao nhất không được đặt liên tiếp" CÓ tồn tại trong code backend** (`RejectReason.ALREADY_HIGHEST_BIDDER`, `409`), và có thêm quy tắc **"người bán không được đấu giá sản phẩm của chính mình"** (`SELLER_CANNOT_BID`, `409`).
- Bảng "đề xuất mã lỗi" ở bản trước (dựa trên `MockAuctionApi`) đã được thay bằng bảng thực tế ở trên.

**Hành vi FE khi gặp lỗi** *(theo bản trước, chưa kiểm tra lại — Android source không có trong môi trường kiểm tra này)*: `AuctionApi.placeBid` khai báo `suspend fun placeBid(...): BidResponse` (không bọc `Response<>`), nên mọi HTTP không phải 2xx làm Retrofit ném `HttpException`; bản trước ghi rằng FE hiển thị `"Lỗi đặt giá: ${e.message}"`. Vì backend luôn trả body `{"statusCode","message"}`, FE cần đọc `message` từ `HttpException.response()?.errorBody()` để hiển thị đúng thông điệp (ví dụ "Bid amount must be higher than ..."); nếu chỉ dùng `e.message` thì sẽ chỉ thấy `"HTTP 409"`. Trạng thái hiện tại của phần này ở app (đã đọc error body hay chưa) chưa được kiểm tra lại. Nhánh FE `"Đặt giá không được chấp nhận."` dựa trên `accepted == false` trong response 200 thực tế sẽ không bao giờ chạy với backend hiện tại.

### 4.3 Validate `amount` phía backend — không tin phía client

Vì `amount` do FE tính sẵn (`currentPrice_tại_thời_điểm_load_màn_hình + step`), giữa lúc user xem màn hình và lúc bấm xác nhận có thể đã có người khác đặt giá khác — **backend tự đọc giá hiện tại trong transaction (optimistic lock qua `AuctionState.version`, retry có backoff) và tự quyết định, không tin số `amount` là "chắc chắn cao nhất"**:
- Trạng thái đấu giá khởi tạo (ở lần bid đầu tiên) với `currentPrice = startPrice`. Do đó **mọi bid, kể cả bid đầu tiên, phải `amount > currentPrice`** — bid đầu tiên bằng đúng `startPrice` cũng bị từ chối (bản trước ghi `>= startPrice` là không đúng với code hiện tại).
- Nếu chấp nhận: lưu `Bid(accepted = true)`, cập nhật `currentPrice = amount`, `currentBidderId = bidderId`, tăng `version`. Nếu 2 bid đồng thời xung đột version thì bên thua được retry và sẽ bị đánh giá lại theo giá mới (có thể thành `409 ... must be higher than the current price`).
- Nếu bị từ chối vì các lý do ở bảng 4.2 (kết thúc, người bán, đang dẫn đầu, giá thấp): lưu `Bid(accepted = false)` rồi trả `409`, để lịch sử ở mục 3.7 có đủ các lần thử.

## 5. Khoảng trống còn lại và ghi chú

- **`category` là free-text**, không phải enum cố định — `ProductRegisterScreen` cho gõ tay; backend mặc định `"OTHER"` nếu không gửi và không validate theo whitelist. Dữ liệu mẫu dùng `ELECTRONICS/WATCHES/ART/VEHICLES/ANTIQUES`.
- **Thời gian đấu giá thuộc về từng sản phẩm** (khái niệm `Event` đã bị loại bỏ hoàn toàn khỏi backend; không còn endpoint/DTO nào liên quan Event và không còn field `eventId`). `POST /products` **bắt buộc** `auctionStartAt` và `auctionEndAt` (mục 3.5), và `ProductSummary`/`ProductDetail` trả cả hai (không null). Mọi thời gian dùng ISO-8601 UTC.
- **Lỗi và Retrofit** *(theo bản trước, chưa kiểm tra lại phía FE)*: `HttpException` của Retrofit không tự đọc `message` trong error body. Backend nay luôn trả `{"statusCode":..., "message":"..."}` (mục "Mã lỗi chung"), nên FE hiển thị được thông điệp thật nếu đọc `HttpException.response()?.errorBody()` và parse trường `message`; nếu FE vẫn dùng `e.message` thì chỉ thấy `"HTTP 409"` chung chung. Với các thông báo ảnh hưởng UX (đặt giá, duyệt/từ chối) nên đọc error body.
- **Vượt dung lượng ảnh (>8MB)**: mã lỗi thật chưa xác nhận (xem 3.5b).
- **Email xác thực**: `EmailService` gửi mã qua email; việc email có được gửi thật hay chỉ log phụ thuộc cấu hình môi trường (chưa kiểm tra lại trong tài liệu này).

## 6. Kiến trúc tính năng Upload ảnh sản phẩm

### 6.1 Ảnh thuộc bounded context nào?

Ảnh sản phẩm được lưu và phục vụ **hoàn toàn trong `catalog-service`** (bounded context Listing/Moderation — cùng nơi sở hữu `Product`), **không** tạo thêm 1 "media-service"/"image-service" riêng. Lý do bám sát nguyên tắc đã nêu ở `ARCHITECTURE_DESIGN.md` mục 5.1 (DC3 — *managing microservices complexity*, khảo sát 34.9% gặp phải) và khuyến nghị Finding 12 (*dùng DDD để tránh over-fragmentation*, không nên "1 service cho mọi feature"): ảnh sản phẩm không có nghiệp vụ độc lập nào (không có luồng duyệt ảnh, không có xử lý ảnh phức tạp như resize/CDN ở quy mô đồ án), nó chỉ là 1 thuộc tính bổ sung của `Product` — tách thành service riêng sẽ chỉ thêm 1 network hop và 1 DB nữa mà không tăng giá trị kiến trúc, đúng kiểu "over-fragmentation" mà khảo sát cảnh báo.

### 6.2 Vì sao `bidding-service` (Participating/Won) không trả `imageUrl`

`AuctionStateResponse` (dùng cho `GET /my/bids/participating` và `GET /my/bids/won`) **cố tình không có field `imageUrl`**. Đây là hệ quả trực tiếp của quyết định đã có sẵn ở `ARCHITECTURE_DESIGN.md` mục 4 (dòng "Won list phải biết `auctionEndAt`..."): `bidding-service` chỉ lưu **read-model tối thiểu** (các field thật sự cần cho nghiệp vụ đấu giá: `currentPrice`, `currentBidderId`, `auctionEndAt`), không sao chép thêm dữ liệu hiển thị thuần túy như ảnh — nếu thêm `imageUrl` vào read-model này, `bidding-service` sẽ phải tự lưu thêm 1 field chỉ để hiển thị (không phục vụ nghiệp vụ bidding), đi ngược tinh thần "share-as-little-as-possible" đã chọn. Nếu sau này cần hiển thị ảnh ở 2 màn này, cách đúng kiến trúc là **API composition ở tầng gọi API** (FE tự gọi thêm `GET /products/{id}` hoặc BFF gộp lại), không phải nhân bản field ảnh sang `bidding-service`.

### 6.3 Lưu trữ file — local disk trong container, có Docker volume

`catalog-service` lưu ảnh trên đĩa cục bộ của chính container (`ProductImageStorageService`, thư mục cấu hình qua `catalog.image-storage.base-path`, mặc định `/app/uploads/products`), đặt tên file theo `{productId}.{ext}` (ghi đè khi upload lại). `docker-compose.yml` gắn Docker named volume `catalog-images` vào thư mục này để dữ liệu **không mất khi container restart/redeploy** — đây là lựa chọn phù hợp quy mô đồ án (không cần S3/object storage phân tán); nếu scale nhiều instance `catalog-service` sau này, đây sẽ là điểm cần đổi sang shared object storage (ghi nhận là hướng mở rộng, tương tự các mục đã liệt kê ở `ARCHITECTURE_DESIGN.md` mục 7).

### 6.4 Migration & DTO liên quan

- `V2__add_product_image.sql`: `ALTER TABLE products ADD COLUMN image_path VARCHAR(500) NULL;` — nullable, không phá dữ liệu cũ.
- `Product.imagePath: String?` (domain) → map sang `imageUrl: String?` ở DTO (`ProductResponse`, `ProductSummaryResponse`) dưới dạng đường dẫn tương đối `"products/{id}/image"` (không lộ đường dẫn file thật trên đĩa).
- Gateway: **không cần thêm route mới** — `/api/products/**` là route sẵn có tới `catalog-service`, tự động bao phủ 2 endpoint ảnh, không xung đột với route riêng của `bidding-service` (`/api/products/*/bids`, order nhỏ hơn nên được ưu tiên). Route `catalog-service` còn phục vụ `/api/my/products/**` và `/api/admin/**`; route `auth-service` phục vụ `/api/auth/**` và `/api/users/**`.

## 7. Vòng đời trạng thái sản phẩm — tự động mở/đóng bởi `AuctionScheduler`

`ProductSummary.status`/`ProductDetail.status` nhận 1 trong `PENDING_APPROVAL | APPROVED | REJECTED | ACTIVE | SOLD | ENDED_NO_BID`. Đường chuyển trạng thái:

```
PENDING_APPROVAL --(admin approve)--> APPROVED --(đến auctionStartAt)--> ACTIVE --(đến auctionEndAt)--> SOLD
                                                                                                    \-> ENDED_NO_BID
PENDING_APPROVAL --(admin reject + reason)--> REJECTED
```

- **Duyệt/từ chối** do admin thực hiện (mục 3.9). Admin duyệt sản phẩm đã quá `auctionEndAt` → `409`.
- **Mở phiên** (`APPROVED -> ACTIVE`): job `AuctionScheduler` (catalog-service) tự chuyển khi `now >= auctionStartAt` (và chưa hết giờ). `APPROVED` nghĩa là "đã duyệt, đang chờ giờ bắt đầu" — chưa nhận bid và chưa xuất hiện ở `GET /products`.
- **Đóng phiên**: khi `now >= auctionEndAt`, job chuyển sang **`SOLD`** nếu bidding-service báo có người thắng, hoặc **`ENDED_NO_BID`** nếu chưa từng có bid hợp lệ (áp dụng cho cả sản phẩm `ACTIVE` lẫn sản phẩm còn `APPROVED` mà đã quá giờ kết thúc). Nếu bidding-service không phản hồi thì bỏ qua lượt đó, giữ nguyên trạng thái và thử lại ở lượt sau.
- **Chu kỳ chạy**: mỗi **5 giây** theo mặc định (`auction-closing.interval-ms: 5000`, cấu hình được; batch mặc định 100 sản phẩm/lần), nên độ trễ mở/đóng tối đa xấp xỉ 5 giây — **không còn là "quét mỗi phút"** và job không còn tên `AuctionClosingJob`. An toàn khi chạy nhiều instance (mọi lần ghi là UPDATE có điều kiện theo trạng thái).
- **Tính đúng đắn của bid không phụ thuộc scheduler**: bidding-service tự chặn bid khi `now >= auctionEndAt` và tự kiểm tra sản phẩm phải `ACTIVE` ở lần bid đầu (mục 4.2); job chỉ đồng bộ `Product.status` cho hiển thị/tra cứu.

**Việc FE cần làm khi có thời gian** (chưa bắt buộc cho phạm vi đồ án hiện tại, ghi nhận để không bị bất ngờ khi thấy giá trị lạ trả về):
- Chỗ nào đang so sánh cứng `status == "ACTIVE"`/`"SOLD"` (ví dụ `ProductDetailScreen` quyết định hiện/ẩn Bidding Bar, `ExhibitionManagementScreen`/`ProductSearchScreen` hiển thị badge trạng thái) nên có nhánh cho `APPROVED` (ví dụ "Đã duyệt, chờ mở phiên") và `ENDED_NO_BID` (ví dụ "Hết giờ, không có người đấu giá"), tối thiểu là hiển thị 1 badge/nhãn riêng thay vì rơi vào nhánh mặc định. *(hiện trạng xử lý của FE: theo bản trước, chưa kiểm tra lại)*
- Không có thay đổi nào về shape của `ProductSummary`/`ProductDetail` — vẫn cùng field `status: String`.
- Đây là thay đổi 1 chiều (backend → FE đọc), không có endpoint mới nào FE phải gọi để mở/đóng phiên.
