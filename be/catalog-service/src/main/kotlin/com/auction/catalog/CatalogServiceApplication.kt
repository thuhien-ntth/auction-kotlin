package com.auction.catalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

// @EnableScheduling: bật AuctionScheduler (job/AuctionScheduler.kt) — xử lý gap 3,
// đóng các phiên đấu giá đã hết giờ (xem mục 9.3 ARCHITECTURE_DESIGN.md).
@SpringBootApplication
@EnableCaching
@EnableScheduling
class CatalogServiceApplication

fun main(args: Array<String>) {
    runApplication<CatalogServiceApplication>(*args)
}
