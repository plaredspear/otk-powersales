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

        /** iOS manifest.plist 엔드포인트 경로 (절대 URL 합성용). */
        fun iosManifestPath(baseUrl: String, id: Long): String =
            "$baseUrl/api/v1/mobile/app-package/ios/manifest.plist?id=$id"
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
     * iOS manifest.plist 동적 생성. 요청 시점에 IPA presigned URL 을 발급해 plist XML 에 주입한다.
     */
    fun buildIosManifest(id: Long): String {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        val ipaUrl = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return manifestPlistBuilder.build(
            ipaUrl = ipaUrl,
            bundleIdentifier = entity.bundleIdentifier ?: "",
            bundleVersion = entity.versionName,
            title = IOS_INSTALL_TITLE,
        )
    }

    /**
     * iOS OTA 설치 안내 HTML 페이지 생성. 공유 가능한 https URL 로 열려 페이지 내 버튼이
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
