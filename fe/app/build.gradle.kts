import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

// Đọc cấu hình URL backend từ local.properties (không commit) hoặc gradle property, tránh hard-code IP LAN.
val apiBaseUrl: String = run {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { props.load(it) }
    val url = props.getProperty("api.base.url")
        ?: (project.findProperty("apiBaseUrl") as String?)
        ?: "http://192.168.0.101:8080/api/"
    if (url.endsWith("/")) url else "$url/"
}

android {
    namespace = "com.auction.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.auction.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        // URL backend (qua API Gateway). Thứ tự ưu tiên:
        //   1) local.properties:  api.base.url=http://10.0.2.2:8080/api/
        //   2) gradle property:   -PapiBaseUrl=http://<ip-may-chay-backend>:8080/api/
        //   3) mặc định bên dưới.
        // 10.0.2.2 là alias của "localhost máy host" khi chạy trên Android Emulator;
        // thiết bị thật thì dùng IP LAN của máy chạy docker-compose. Phải kết thúc bằng "/api/".
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Load ảnh sản phẩm từ URL (GET /api/products/{id}/image) — xem ui/components/ProductThumbnail.kt
    implementation("io.coil-kt:coil-compose:2.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
