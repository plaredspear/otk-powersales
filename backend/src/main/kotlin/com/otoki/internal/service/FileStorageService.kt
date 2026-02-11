package com.otoki.internal.service

import com.otoki.internal.exception.FileStorageException
import com.otoki.internal.exception.InvalidFileException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

/**
 * 파일 저장소 Service
 * 현장 점검 사진 등의 파일을 저장하고 URL을 반환한다.
 */
@Service
class FileStorageService(
    @Value("\${app.file.upload-dir:./uploads}")
    private val uploadDir: String,

    @Value("\${app.file.base-url:http://localhost:8080/files}")
    private val baseUrl: String
) {

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
        )
    }

    private val rootLocation: Path = Paths.get(uploadDir)

    init {
        try {
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw FileStorageException("파일 저장 디렉토리 생성 실패", e)
        }
    }

    /**
     * 현장 점검 사진 업로드
     *
     * @param file 업로드 파일
     * @param userId 사용자 ID
     * @param inspectionId 점검 ID (임시 저장 시 null 가능)
     * @return 저장된 파일 URL
     */
    fun uploadInspectionPhoto(
        file: MultipartFile,
        userId: Long,
        inspectionId: Long? = null
    ): String {
        // 파일 검증
        validateFile(file)

        // 파일명 생성 (UUID + 확장자)
        val originalFilename = file.originalFilename ?: "unknown"
        val extension = getFileExtension(originalFilename)
        val filename = "${UUID.randomUUID()}${extension}"

        // 저장 경로 생성: inspections/{userId}/{inspectionId 또는 temp}/{filename}
        val subDir = if (inspectionId != null) "$inspectionId" else "temp"
        val targetLocation = rootLocation
            .resolve("inspections")
            .resolve(userId.toString())
            .resolve(subDir)

        try {
            // 디렉토리 생성
            Files.createDirectories(targetLocation)

            // 파일 저장
            val targetFile = targetLocation.resolve(filename)
            Files.copy(file.inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING)

            // URL 반환
            return "$baseUrl/inspections/$userId/$subDir/$filename"
        } catch (e: IOException) {
            throw FileStorageException("파일 저장 실패: ${file.originalFilename}", e)
        }
    }

    /**
     * 파일 유효성 검증
     */
    private fun validateFile(file: MultipartFile) {
        // 파일 크기 검증
        if (file.size > MAX_FILE_SIZE) {
            throw InvalidFileException("파일 크기가 초과되었습니다 (최대 10MB)")
        }

        // 파일 타입 검증
        val contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw InvalidFileException("지원하지 않는 파일 형식입니다. 이미지 파일만 업로드 가능합니다.")
        }

        // 파일명 검증
        if (file.originalFilename.isNullOrBlank()) {
            throw InvalidFileException("파일명이 올바르지 않습니다")
        }

        // 빈 파일 검증
        if (file.isEmpty) {
            throw InvalidFileException("빈 파일은 업로드할 수 없습니다")
        }
    }

    /**
     * 파일 확장자 추출
     */
    private fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            filename.substring(lastDotIndex)
        } else {
            ""
        }
    }
}
