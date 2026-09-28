-- AuctionState = read-model cục bộ của bidding-service, sao chép 1 lần từ catalog-service
-- khi có bid đầu tiên (auctionEndAt tĩnh, không đổi) — tránh gọi cross-service cho mỗi
-- lần đọc "won list" (ARCHITECTURE_DESIGN.md mục 4). currentPrice/currentBidderId là
-- nguồn ghi (source of truth) DUY NHẤT cho giá — optimistic locking qua cột version.
CREATE TABLE auction_state (
    product_id        UUID PRIMARY KEY,
    seller_id         UUID NOT NULL,
    current_price     NUMERIC(14,2) NOT NULL,
    current_bidder_id UUID NULL,
    auction_end_at    TIMESTAMPTZ NOT NULL,
    version           BIGINT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bids (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES auction_state(product_id),
    bidder_id   UUID NOT NULL,
    amount      NUMERIC(14,2) NOT NULL,
    accepted    BOOLEAN NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bids_product ON bids(product_id, created_at DESC);
CREATE INDEX idx_bids_bidder ON bids(bidder_id);
-- phục vụ "Won list": tìm nhanh các product mà mình đang là currentBidder
CREATE INDEX idx_auction_state_current_bidder ON auction_state(current_bidder_id);
