package com.otoki.powersales.platform.apppackage.dto

import com.otoki.powersales.platform.apppackage.entity.AppPlatform

/**
 * 모바일 버전 체크 응답.
 *
 * - [updateAvailable]: 최신 version_code 가 요청 version_code 보다 큼.
 * - [forceUpdate]: 요청 버전 초과 ~ 최신 사이에 강제 버전이 존재 → 진입 차단 대상.
 * - [downloadUrl]: Android = APK presigned URL, iOS = `itms-services://...` (manifest 엔드포인트 주입).
 */
data class AppVersionCheckDto(
    val platform: AppPlatform,
    val updateAvailable: Boolean,
    val forceUpdate: Boolean,
    val latestVersionName: String?,
    val latestVersionCode: Long?,
    val releaseNote: String?,
    val downloadUrl: String?,
)

/** Android 다운로드 URL 재발급 응답. */
data class AppPackageDownloadUrlDto(
    val url: String,
    val expiresInSeconds: Int,
)
