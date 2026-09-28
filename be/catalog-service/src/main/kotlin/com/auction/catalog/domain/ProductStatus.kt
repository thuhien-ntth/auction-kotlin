package com.auction.catalog.domain

/**
 * State machine của sản phẩm (không còn khái niệm Event; thời gian đấu giá nằm trên chính sản phẩm):
 *
 * PENDING_APPROVAL -> APPROVED -> ACTIVE -> SOLD
 *                                        -> ENDED_NO_BID
 *                  -> REJECTED
 *
 * APPROVED = admin đã duyệt, chờ đến auctionStartAt. ACTIVE = đang trong [auctionStartAt, auctionEndAt),
 * chỉ trạng thái này mới nhận bid (bidding-service kiểm tra qua GET /internal/products/id).
 * APPROVED -> ACTIVE và ACTIVE -> SOLD / ENDED_NO_BID đều do AuctionScheduler (job/AuctionScheduler.kt)
 * tự chuyển theo đồng hồ; SOLD khi có người thắng, ENDED_NO_BID khi không có bid hợp lệ nào.
 */
enum class ProductStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ACTIVE,
    SOLD,
    ENDED_NO_BID
}
