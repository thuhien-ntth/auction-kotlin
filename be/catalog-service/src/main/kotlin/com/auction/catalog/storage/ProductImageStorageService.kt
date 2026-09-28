package com.auction.catalog.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Lưu ảnh sản phẩm cục bộ trên disk của chính catalog-service (Docker volume riêng, xem
 * docker-compose.yml). Ảnh là dữ liệu thuộc bounded context Catalog (gắn liền 1-1 với Product),
 * nên KHÔNG tách thành 1 "media-service" riêng — tách ra sẽ chỉ thêm 1 network hop mà không
 * giải quyết boundary nào (đúng tinh thần "không thiết kế 1 service cho mọi feature",
 * ARCHITECTURE_DESIGN.md mục 5.1 DC3). Nếu sau này cần scale ảnh lớn/CDN thật, đây là chỗ
 * thay implementation (S3/MinIO) mà không phải đổi hợp đồng API (vẫn là GET .../image).
 */
@Component
class ProductImageStorageService(
    @Value("\${catalog.image-storage.base-path:/app/uploads/products}") basePathProperty: String
) {
    private val basePath: Path = Paths.get(basePathProperty).also { Files.createDirectories(it) }

    private val allowedContentTypes = mapOf(
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp"
    )

    fun save(productId: UUID, file: MultipartFile): String {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File ảnh rỗng")
        }
        val ext = allowedContentTypes[file.contentType?.lowercase()]
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only JPEG/PNG/WebP images are supported (received: ${file.contentType})"
            )
        // Tên file = productId — mỗi sản phẩm chỉ giữ 1 ảnh, upload lại sẽ ghi đè (không tích luỹ rác trên disk).
        val filename = "$productId.$ext"
        val target = basePath.resolve(filename)
        file.inputStream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return filename
    }

    fun load(filename: String): Resource {
        val file = basePath.resolve(filename).normalize()
        if (!file.startsWith(basePath) || !Files.exists(file)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh")
        }
        return UrlResource(file.toUri())
    }

    fun contentTypeFor(filename: String): MediaType = when (filename.substringAfterLast('.', "").lowercase()) {
        "png" -> MediaType.IMAGE_PNG
        "webp" -> MediaType.parseMediaType("image/webp")
        else -> MediaType.IMAGE_JPEG
    }
}
