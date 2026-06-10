package com.otoki.powersales.apppackage.dto

import com.otoki.powersales.apppackage.entity.AppPackage
import com.otoki.powersales.apppackage.entity.AppPlatform
import java.time.LocalDateTime

/** 앱 패키지 목록 항목 (파일 다운로드 URL 미포함 — 목록은 발급 비용 회피). */
data class AppPackageListItemDto(
    val id: Long,
    val platform: AppPlatform,
    val versionName: String,
    val versionCode: Long,
    val forceUpdate: Boolean,
    val isLatest: Boolean,
    val releaseNote: String?,
    val fileName: String,
    val fileSize: Long,
    val bundleIdentifier: String?,
    val uploadedAt: LocalDateTime,
) {
    companion object {
        fun from(e: AppPackage): AppPackageListItemDto = AppPackageListItemDto(
            id = e.id,
            platform = e.platform,
            versionName = e.versionName,
            versionCode = e.versionCode,
            forceUpdate = e.forceUpdate,
            isLatest = e.isLatest,
            releaseNote = e.releaseNote,
            fileName = e.fileName,
            fileSize = e.fileSize,
            bundleIdentifier = e.bundleIdentifier,
            uploadedAt = e.createdAt,
        )
    }
}

/** 앱 패키지 상세 (presigned 다운로드 URL 포함). */
data class AppPackageDetailDto(
    val id: Long,
    val platform: AppPlatform,
    val versionName: String,
    val versionCode: Long,
    val forceUpdate: Boolean,
    val isLatest: Boolean,
    val releaseNote: String?,
    val fileName: String,
    val fileSize: Long,
    val bundleIdentifier: String?,
    val downloadUrl: String,
    val uploadedAt: LocalDateTime,
) {
    companion object {
        fun from(e: AppPackage, downloadUrl: String): AppPackageDetailDto = AppPackageDetailDto(
            id = e.id,
            platform = e.platform,
            versionName = e.versionName,
            versionCode = e.versionCode,
            forceUpdate = e.forceUpdate,
            isLatest = e.isLatest,
            releaseNote = e.releaseNote,
            fileName = e.fileName,
            fileSize = e.fileSize,
            bundleIdentifier = e.bundleIdentifier,
            downloadUrl = downloadUrl,
            uploadedAt = e.createdAt,
        )
    }
}

data class AppPackageForceUpdateRequest(
    val forceUpdate: Boolean,
)

data class AppPackageReleaseNoteUpdateRequest(
    val releaseNote: String?,
)
