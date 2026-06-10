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

/**
 * 앱 패키지 상세 (presigned 다운로드 URL 포함).
 *
 * [downloadUrl] 은 [downloadUrlExpiresInSeconds] 동안만 유효한 임시 서명 URL 이다.
 * 발급 시점(조회 응답 시각) + 만료초 = 만료 시각. 웹은 이를 클립보드 복사 시 안내 문구에 활용한다.
 */
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
    val downloadUrlExpiresInSeconds: Int,
    /**
     * iOS 고정 OTA 설치 안내 페이지 URL (mobile API public 도메인 기준, 항상 최신 버전).
     * Android 는 null. web 은 이 값을 그대로 복사/공유한다 — web admin 도메인(IP whitelist)이
     * 아니라 사외 접근 가능한 API 도메인이어야 영업사원이 설치할 수 있기 때문.
     */
    val iosInstallUrl: String?,
    val uploadedAt: LocalDateTime,
) {
    companion object {
        fun from(
            e: AppPackage,
            downloadUrl: String,
            downloadUrlExpiresInSeconds: Int,
            iosInstallUrl: String? = null,
        ): AppPackageDetailDto =
            AppPackageDetailDto(
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
                downloadUrlExpiresInSeconds = downloadUrlExpiresInSeconds,
                iosInstallUrl = iosInstallUrl,
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
