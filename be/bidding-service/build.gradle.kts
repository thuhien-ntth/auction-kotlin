plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Actuator + Micrometer: /actuator/health, /actuator/metrics (bidding.bids, bidding.optimistic.conflicts...)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Bắt buộc phải có để Jackson hiểu ngữ nghĩa non-null/default-value của Kotlin data class —
    // thiếu module này, deserialize 1 DTO có field non-null (ví dụ CatalogProductInfo, BidResponse)
    // sẽ ném NullPointerException ở tầng Jackson dù JSON server trả về hoàn toàn đúng. Phát hiện
    // khi viết integration test cho Manual Bidding (ManualBiddingHappyPathTest) — xem
    // ARCHITECTURE_DESIGN.md mục 10 (kết quả chạy test) để biết chi tiết.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // H2 in-memory — chỉ dùng khi chạy `test` (src/test/resources/application.yml trỏ datasource
    // sang H2), không ảnh hưởng runtime thật (vẫn dùng Postgres qua application.yml chính).
    testImplementation("com.h2database:h2")
}
