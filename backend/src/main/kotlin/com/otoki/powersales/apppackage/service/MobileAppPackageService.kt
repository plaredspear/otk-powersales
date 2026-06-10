package com.otoki.powersales.apppackage.service

import com.otoki.powersales.apppackage.dto.AppPackageDownloadUrlDto
import com.otoki.powersales.apppackage.dto.AppVersionCheckDto
import com.otoki.powersales.apppackage.entity.AppPackage
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.exception.AppPackageNotFoundException
import com.otoki.powersales.apppackage.repository.AppPackageRepository
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
@Transactional(readOnly = true)
class MobileAppPackageService(
    private val appPackageRepository: AppPackageRepository,
    private val storageService: StorageService,
    private val manifestPlistBuilder: ManifestPlistBuilder,
    private val iosInstallPageBuilder: IosInstallPageBuilder,
) {

    companion object {
        private const val IOS_INSTALL_TITLE = "오뚜기 파워세일즈"

        private const val IOS_BASE = "/api/v1/mobile/app-package/ios"

        /** iOS manifest.plist 엔드포인트 경로 (특정 버전, 절대 URL 합성용). */
        fun iosManifestPath(baseUrl: String, id: Long): String =
            "$baseUrl$IOS_BASE/manifest.plist?id=$id"

        /** iOS manifest.plist 엔드포인트 경로 (항상 최신 isLatest 버전). 고정 링크용. */
        fun iosLatestManifestPath(baseUrl: String): String =
            "$baseUrl$IOS_BASE/manifest.plist/latest"

        /** iOS OTA 설치 안내 페이지 경로 (항상 최신 isLatest 버전). 고정 배포 링크. */
        fun iosLatestInstallPath(baseUrl: String): String =
            "$baseUrl$IOS_BASE/install/latest"

        private const val ANDROID_BASE = "/api/v1/mobile/app-package/android"

        /**
         * Android 최신 APK 다운로드 고정 경로. 이 URL 로 접근하면 backend 가 최신 isLatest APK 의
         * presigned URL 로 302 redirect 한다. iOS 와 달리 안내 페이지 없이 APK 를 직접 받아 설치.
         */
        fun androidLatestDownloadPath(baseUrl: String): String =
            "$baseUrl$ANDROID_BASE/download/latest"
    }

    /**
     * 버전 체크. 최신 지정 우선, 없으면 version_code 최대값 fallback.
     *
     * @param baseUrl iOS manifest 엔드포인트 절대 URL 합성을 위한 요청 기준 origin (예: https://api.example.com)
     */
    fun check(platform: AppPlatform, versionCode: Long, baseUrl: String): AppVersionCheckDto {
        val latest = resolveLatest(platform)
            ?: return AppVersionCheckDto(
                platform = platform,
                updateAvailable = false,
                forceUpdate = false,
                latestVersionName = null,
                latestVersionCode = null,
                releaseNote = null,
                downloadUrl = null,
            )

        val updateAvailable = latest.versionCode > versionCode
        // 요청 버전 초과 ~ 최신 사이에 강제 버전이 하나라도 있으면 강제 (중간 강제 버전 우회 방지).
        val forceUpdate = updateAvailable &&
            appPackageRepository.existsByPlatformAndForceUpdateTrueAndVersionCodeGreaterThan(platform, versionCode)

        val downloadUrl = if (updateAvailable) buildDownloadUrl(latest, baseUrl) else null

        return AppVersionCheckDto(
            platform = platform,
            updateAvailable = updateAvailable,
            forceUpdate = forceUpdate,
            latestVersionName = latest.versionName,
            latestVersionCode = latest.versionCode,
            releaseNote = latest.releaseNote,
            downloadUrl = downloadUrl,
        )
    }

    /** Android APK presigned 다운로드 URL 재발급. */
    fun issueDownloadUrl(id: Long): AppPackageDownloadUrlDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return AppPackageDownloadUrlDto(url = url, expiresInSeconds = StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    /**
     * 최신 Android APK 의 presigned 다운로드 URL 을 발급한다. 고정 다운로드 엔드포인트가 이 URL 로
     * 302 redirect 하므로, 영구 고정 링크 뒤에서 만료되는 presigned URL 을 매 요청 새로 발급한다.
     * 최신 APK 부재 시 NotFound.
     */
    fun issueLatestAndroidDownloadUrl(): String {
        val entity = resolveLatest(AppPlatform.ANDROID) ?: throw AppPackageNotFoundException()
        return storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    /**
     * iOS manifest.plist 동적 생성(특정 버전). 요청 시점에 IPA presigned URL 을 발급해 plist XML 에 주입한다.
     */
    fun buildIosManifest(id: Long): String =
        buildManifestFor(appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() })

    /** iOS manifest.plist 동적 생성(항상 최신 isLatest 버전). 고정 링크용. */
    fun buildLatestIosManifest(): String = buildManifestFor(requireLatestIos())

    /**
     * iOS OTA 설치 안내 HTML 페이지 생성(특정 버전). 공유 가능한 https URL 로 열려 페이지 내 버튼이
     * Safari 컨텍스트에서 itms-services 를 호출하게 한다.
     *
     * @param baseUrl manifest 절대 URL 합성용 요청 기준 origin
     */
    fun buildIosInstallPage(id: Long, baseUrl: String): String {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        return iosInstallPageBuilder.build(
            manifestUrl = iosManifestPath(baseUrl, id),
            title = IOS_INSTALL_TITLE,
            versionName = entity.versionName,
        )
    }

    /**
     * iOS OTA 설치 안내 HTML 페이지 생성(항상 최신 isLatest 버전). 고정 배포 링크가 가리키는 페이지.
     * 새 버전 업로드 후 "최신 지정"만 하면 동일 링크가 신버전을 가리킨다.
     */
    fun buildLatestIosInstallPage(baseUrl: String): String {
        val entity = requireLatestIos()
        return iosInstallPageBuilder.build(
            manifestUrl = iosLatestManifestPath(baseUrl),
            title = IOS_INSTALL_TITLE,
            versionName = entity.versionName,
        )
    }

    private fun buildManifestFor(entity: AppPackage): String {
        val ipaUrl = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return manifestPlistBuilder.build(
            ipaUrl = ipaUrl,
            bundleIdentifier = entity.bundleIdentifier ?: "",
            bundleVersion = entity.versionName,
            title = IOS_INSTALL_TITLE,
        )
    }

    /** 최신 iOS 패키지. 없으면 NotFound (아직 배포본이 없는 상태). */
    private fun requireLatestIos(): AppPackage =
        resolveLatest(AppPlatform.IOS) ?: throw AppPackageNotFoundException()

    private fun resolveLatest(platform: AppPlatform): AppPackage? =
        appPackageRepository.findByPlatformAndIsLatestTrue(platform)
            ?: appPackageRepository.findTopByPlatformOrderByVersionCodeDesc(platform)

    private fun buildDownloadUrl(latest: AppPackage, baseUrl: String): String = when (latest.platform) {
        AppPlatform.ANDROID ->
            storageService.getPresignedUrl(latest.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        AppPlatform.IOS -> {
            val encoded = URLEncoder.encode(iosManifestPath(baseUrl, latest.id), StandardCharsets.UTF_8)
            "itms-services://?action=download-manifest&url=$encoded"
        }
    }
}
