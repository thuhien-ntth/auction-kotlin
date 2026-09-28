# Auction System — Frontend (Jetpack Compose)

Khung app Android tối thiểu cho đúng 8 màn hình trong phạm vi đã chốt (xem `../ARCHITECTURE_DESIGN.md` mục 1). Gọi API qua `be/api-gateway` (`http://10.0.2.2:8080/api/...` khi chạy trên Android Emulator).

## ⚠️ Tình trạng đã kiểm tra

Cũng như `be/`, mã nguồn được viết và rà soát thủ công (import, kiểu dữ liệu, tên field JSON khớp với DTO backend) trong sandbox cloud **bị chặn egress tới `dl.google.com`/Maven Central**, nên **chưa build/chạy thật được** trong lúc tạo khung code này. Khi mở project trên Android Studio (máy có Internet bình thường):
1. Gradle sẽ tự tải dependency lần đầu (Compose BOM, Retrofit, Navigation, DataStore...).
2. Chạy `./gradlew assembleDebug` hoặc bấm Run — sửa lỗi biên dịch nhỏ nếu có (khả năng cao là version plugin Android Gradle chưa khớp với Android Studio đang dùng, xem `build.gradle.kts` gốc).
3. Cần khởi động `be/` (`docker compose up --build`) trước, rồi mới chạy app.

## Cấu trúc

```
fe/
  app/src/main/kotlin/com/auction/app/
    AppContainer.kt          # DI thủ công (không Hilt) — tokenStore + api dùng chung
    MainActivity.kt
    data/TokenStore.kt        # Lưu JWT bằng DataStore Preferences
    network/                  # Retrofit + OkHttp + model DTO (network/model/Models.kt)
    ui/theme/                 # Color.kt, Theme.kt, Type.kt — Material Design 3 theme (navy + vàng đồng)
    ui/navigation/            # Routes.kt (8 route) + AppNavHost.kt
    ui/components/            # AppScaffold (TopAppBar+BottomNav), StatusChip, ProductThumbnail
    ui/screens/                # 8 Composable, 1 file/màn hình
```

## 8 màn hình đã hiện thực (khớp `Routes.kt`)

| Route | Màn hình | Ghi chú |
|---|---|---|
| `login` | LoginScreen | Đăng nhập, lưu JWT vào DataStore |
| `product_search` | ProductSearchScreen | Auction: Product search — tìm sản phẩm ACTIVE |
| `auction_detail/{eventId}` | AuctionDetailScreen | Auction: Auction detail — thông tin Event + sản phẩm trong event |
| `product_detail/{productId}` | ProductDetailScreen | Auction: Product detail — xem giá hiện tại, đặt giá, lịch sử bid |
| `exhibition_management` | ExhibitionManagementScreen | My page: Exhibition Management — sản phẩm đã đăng |
| `product_register` | ProductRegisterScreen | My page: Exhibition product register — đăng sản phẩm mới (→ PENDING_APPROVAL) |
| `participating` | ParticipatingScreen | Product list: Participating |
| `won` | WonScreen | Product list: Won |

Không có màn "Memo list" — đã loại khỏi scope theo xác nhận của người dùng.

## Giao diện — Material Design 3, tham khảo mockup spec gốc

UI dùng Material Design 3 (Compose Material3) thay vì tự vẽ layout tuỳ ý. Bảng màu (navy + vàng đồng), bố cục header/card sản phẩm, và badge trạng thái dạng pill được tham khảo trực tiếp từ mockup "NJ AUCTION" trong file spec (`HB4_1334_Auction System_User_Basic design...xlsx`, các sheet `USATS0020000 Login`, `USTPS0010000 Auction List`, `USEDS0010000 Auction detail`, `USEDS0030000 Product detail`, `USMPS0020000/0050000 Bidding/Won List`, `USMPS0080000 Sellers Products`, `USRPS...`), không tự bịa phong cách:

- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` — bảng màu light/dark riêng cho app (primary = navy giống header mockup, secondary = vàng đồng giống banner "Highest Bidder"), tắt Material You dynamic color mặc định để giữ đúng màu thương hiệu.
- `ui/components/AppScaffold.kt` — TopAppBar tông navy + bottom navigation có icon Material (`material-icons-extended`), phỏng theo header "NJ AUCTION" trong mọi mockup.
- `ui/components/StatusChip.kt` — badge trạng thái sản phẩm dạng pill bo tròn, phỏng theo cách hiển thị 承認/不承認/新規 (đã duyệt/từ chối/chờ duyệt) ở màn "出品申請一覧" trong spec.
- `ui/components/ProductThumbnail.kt` — ảnh đại diện sản phẩm dạng placeholder (icon búa đấu giá trên nền tonal) vì model backend không có field ảnh (ngoài phạm vi đồ án: upload/lưu trữ ảnh) — giữ đúng bố cục thumbnail vuông bo góc như card sản phẩm trong mockup nhưng không bịa URL ảnh giả.
- Màn Product Detail có banner vàng "Bạn đang giữ giá cao nhất" khi user hiện tại là người đặt giá được chấp nhận gần nhất — phỏng theo banner "You are the current Highest Bidder" trong mockup Product Detail.

## Thiết kế đơn giản có chủ đích (khung tối thiểu)

- Không dùng ViewModel/StateFlow/Hilt — state cục bộ bằng `remember`/`mutableStateOf`, gọi API trực tiếp trong `LaunchedEffect`/`rememberCoroutineScope` bên trong Composable. Đủ để chứng minh luồng gọi API đúng kiến trúc, không lặp lại toàn bộ khung best-practice production.
- Model ở `network/model/Models.kt` khai báo riêng, KHÔNG share code với backend — đúng tinh thần "không chia sẻ contract giữa các service" đã chọn trong thiết kế (mục 4, hàng "Ranh giới service").
- `AndroidManifest.xml` bật `usesCleartextTraffic="true"` vì gateway chạy `http://` khi test cục bộ (Android chặn cleartext mặc định từ API 28).

## Tài khoản demo

Dùng chung với backend (`be/auth-service/.../V2__seed.sql`), mật khẩu `Passw0rd!`:
- `bidder1@auction.local` (điền sẵn ở màn Login) — dùng để test Product search / đặt giá / Participating / Won
- `seller1@auction.local` — dùng để test đăng sản phẩm / Exhibition Management
- `admin@auction.local` — duyệt sản phẩm/tạo Event qua `curl` (xem `be/README.md`), app không có màn Admin riêng (đúng chủ đích "lát cắt Admin tối thiểu")
