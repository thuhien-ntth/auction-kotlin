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
