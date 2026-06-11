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

/**
 * 대규모 배포용 고정 설치/다운로드 링크 응답. 각 url 이 null 이면 API 도메인 미설정 환경.
 *
 * 고정 링크가 실제로 가리키는 "현재 최신 지정 버전"을 함께 내려 web 이 링크 옆에 표시한다.
 * 링크 해석은 mobile 다운로드/설치 엔드포인트와 동일한 resolveLatest(최신 지정 우선,
 * 없으면 versionCode 최대값)를 따른다. 해당 플랫폼 패키지가 하나도 없으면 버전 필드는 null.
 *
 * @property iosInstallUrl iOS OTA 설치 안내 페이지 고정 링크 (Safari 에서 열어 설치)
 * @property androidDownloadUrl Android 최신 APK 다운로드 고정 링크 (직접 받아 설치)
 * @property iosLatestVersionName iOS 고정 링크가 가리키는 현재 버전명 (예: 1.2.3). 패키지 부재 시 null
 * @property iosLatestVersionCode iOS 고정 링크가 가리키는 현재 버전 코드. 패키지 부재 시 null
 * @property androidLatestVersionName Android 고정 링크가 가리키는 현재 버전명. 패키지 부재 시 null
 * @property androidLatestVersionCode Android 고정 링크가 가리키는 현재 버전 코드. 패키지 부재 시 null
 */
data class AppPackageDistributionUrlsDto(
    val iosInstallUrl: String?,
    val androidDownloadUrl: String?,
    val iosLatestVersionName: String?,
    val iosLatestVersionCode: Long?,
    val androidLatestVersionName: String?,
    val androidLatestVersionCode: Long?,
)

data class AppPackageForceUpdateRequest(
    val forceUpdate: Boolean,
)

data class AppPackageReleaseNoteUpdateRequest(
    val releaseNote: String?,
)
