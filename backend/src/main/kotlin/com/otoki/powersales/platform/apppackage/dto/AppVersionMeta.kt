package com.otoki.powersales.platform.apppackage.dto

/**
 * 버전 체크의 캐시 대상 메타 — 플랫폼별 최신 버전 정보 + 강제 업데이트 경계.
 *
 * presigned downloadUrl 처럼 요청·시점 의존(매번 신선 발급 필요)인 값은 포함하지 않는다.
 * 이 메타만 Redis 에 캐시하고, downloadUrl 은 캐시 밖에서 매 요청 합성한다.
 *
 * - [hasLatest]: 배포본이 하나라도 있는지. false 면 [latestVersionCode] 등은 모두 null.
 * - [latestPackageId]: iOS itms-services manifest URL 합성에 필요한 최신 패키지 id.
 * - [maxForceUpdateVersionCode]: force_update=true 버전들의 최대 version_code (없으면 null).
 *   `요청 versionCode < maxForceUpdateVersionCode` 이면 그 사이에 강제 버전이 존재한다는 뜻.
 */
data class AppVersionMeta(
    val hasLatest: Boolean,
    val latestPackageId: Long?,
    val latestVersionName: String?,
    val latestVersionCode: Long?,
    val latestFileUniqueKey: String?,
    val releaseNote: String?,
    val maxForceUpdateVersionCode: Long?,
) {
    companion object {
        /** 배포본이 없는 상태. */
        val EMPTY = AppVersionMeta(
            hasLatest = false,
            latestPackageId = null,
            latestVersionName = null,
            latestVersionCode = null,
            latestFileUniqueKey = null,
            releaseNote = null,
            maxForceUpdateVersionCode = null,
        )
    }
}
