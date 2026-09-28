pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "auction-system"

include(
    "api-gateway",
    "auth-service",
    "catalog-service",
    "bidding-service"
)
