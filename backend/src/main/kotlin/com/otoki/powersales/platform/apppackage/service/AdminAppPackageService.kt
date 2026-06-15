package com.otoki.powersales.platform.apppackage.service

import com.otoki.powersales.platform.apppackage.dto.AppPackageDetailDto
import com.otoki.powersales.platform.apppackage.dto.AppPackageListItemDto
import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.apppackage.repository.AppPackageRepository
import com.otoki.powersales.platform.common.config.DomainProperties
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.apppackage.entity.AppPackage
import com.otoki.powersales.platform.apppackage.exception.AppPackageBundleIdentifierRequiredException
import com.otoki.powersales.platform.apppackage.exception.AppPackageEnvironmentMismatchException
import com.otoki.powersales.platform.apppackage.exception.AppPackageCannotDeleteLatestException
import com.otoki.powersales.platform.apppackage.exception.AppPackageDuplicateVersionException
import com.otoki.powersales.platform.apppackage.exception.AppPackageFileRequiredException
import com.otoki.powersales.platform.apppackage.exception.AppPackageInvalidExtensionException
import com.otoki.powersales.platform.apppackage.exception.AppPackageNotFoundException
import com.otoki.powersales.platform.apppackage.exception.AppPackageVersionRequiredException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class AdminAppPackageService(
    private val appPackageRepository: AppPackageRepository,
    private val storageService: StorageService,
    private val ipaMetadataExtractor: IpaMetadataExtractor,
    private val apkMetadataExtractor: ApkMetadataExtractor,
    private val domainProperties: DomainProperties,
    private val appVersionMetaProvider: AppVersionMetaProvider,
    @Value("\${app.stage:local}") private val stage: String,
) {

    fun list(platform: AppPlatform?, pageable: Pageable): Page<AppPackageListItemDto> {
        val page = if (platform != null) {
            appPackageRepository.findByPlatform(platform, pageable)
        } else {
            appPackageRepository.findAll(pageable)
        }
        return page.map { AppPackageListItemDto.Companion.from(it) }
    }

    fun getDetail(id: Long): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return AppPackageDetailDto.Companion.from(
            entity,
            url,
            StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS,
            iosInstallUrl = iosInstallUrlOrNull(entity.platform),
        )
    }

    /**
     * iOS 패키지면 대규모 배포용 고정 설치 링크(API public 도메인, 항상 최신 버전)를 반환. Android 는 null.
     * API 도메인 미설정(local)이면 null (web 은 이 경우 버튼을 비활성/숨김 처리).
     */
    private fun iosInstallUrlOrNull(platform: AppPlatform): String? {
        if (platform != AppPlatform.IOS) return null
        return iosInstallUrl()
    }

    /**
     * iOS 대규모 배포용 고정 설치 링크. 버전·패키지와 무관한 고정값(API 도메인만 의존)이라
     * web 이 페이지 상단에 상시 표시한다. API 도메인 미설정(local)이면 null.
     */
    fun iosInstallUrl(): String? {
        val base = apiBaseUrlOrNull() ?: return null
        return MobileAppPackageService.iosLatestInstallPath(base)
    }

    /**
     * Android 대규모 배포용 고정 다운로드 링크. 최신 APK 로 302 redirect 하는 고정 엔드포인트.
     * 버전과 무관한 고정값. API 도메인 미설정(local)이면 null.
     */
    fun androidDownloadUrl(): String? {
        val base = apiBaseUrlOrNull() ?: return null
        return MobileAppPackageService.androidLatestDownloadPath(base)
    }

    /** API public 도메인 기준 https base URL. 미설정(local)이면 null. */
    private fun apiBaseUrlOrNull(): String? {
        val apiDomain = domainProperties.api
        return if (apiDomain.isBlank()) null else "https://$apiDomain"
    }

    /**
     * 대규모 배포용 고정 링크가 실제로 가리키는 "현재 최신 지정" 패키지의 버전명/코드.
     * mobile 다운로드/설치 엔드포인트와 동일한 resolveLatest 로직(최신 지정 우선, 없으면 versionCode
     * 최대값)을 [AppVersionMetaProvider] 의 캐시된 메타로 재사용한다 — 추가 DB 조회 없음.
     * 해당 플랫폼 패키지가 하나도 없으면 (versionName, versionCode) 둘 다 null.
     */
    fun latestVersion(platform: AppPlatform): Pair<String?, Long?> {
        val meta = appVersionMetaProvider.loadMeta(platform)
        return meta.latestVersionName to meta.latestVersionCode
    }

    /**
     * iOS/Android 모두 업로드 파일(.ipa/.apk)에서 식별자·버전을 자동 추출한다.
     * 추출값 우선, 없으면 입력값으로 fallback. 둘 다 없으면 예외.
     *
     * - iOS: .ipa Info.plist → CFBundleIdentifier / CFBundleShortVersionString / CFBundleVersion.
     *        (수동 입력 오타로 OTA manifest 불일치(설치 실패)하는 것을 원천 차단)
     * - Android: .apk AndroidManifest → package(applicationId) / versionName / versionCode.
     *
     * @param versionName 미입력 허용. 추출 실패 + 미입력이면 예외.
     * @param versionCode 미입력 허용. 추출 불가(iOS 비정수 등) + 미입력이면 예외.
     */
    @Transactional
    fun upload(
        platform: AppPlatform,
        versionName: String?,
        versionCode: Long?,
        forceUpdate: Boolean,
        releaseNote: String?,
        file: MultipartFile,
        uploadedById: Long?,
    ): AppPackageDetailDto {
        if (file.isEmpty || file.originalFilename.isNullOrBlank()) throw AppPackageFileRequiredException()
        validateExtension(platform, file.originalFilename!!)

        val fileBytes = file.bytes

        val bundleIdentifier: String?
        val resolvedVersionName: String
        val resolvedVersionCode: Long
        if (platform == AppPlatform.IOS) {
            val meta = ipaMetadataExtractor.extract(fileBytes)
                ?: throw AppPackageBundleIdentifierRequiredException()
            bundleIdentifier = meta.bundleIdentifier
            resolvedVersionName = meta.shortVersion ?: versionName?.ifBlank { null }
                ?: throw AppPackageVersionRequiredException("versionName")
            resolvedVersionCode = meta.bundleVersion?.toLongOrNull() ?: versionCode
                ?: throw AppPackageVersionRequiredException("versionCode")
        } else {
            // Android: AndroidManifest 추출 실패 시(손상/비APK) 입력값으로 fallback 가능하므로
            // null 을 허용한다(파일 검증은 validateExtension 가 이미 수행).
            val meta = apkMetadataExtractor.extract(fileBytes)
            bundleIdentifier = meta?.packageName
            resolvedVersionName = meta?.versionName ?: versionName?.ifBlank { null }
                ?: throw AppPackageVersionRequiredException("versionName")
            resolvedVersionCode = meta?.versionCode ?: versionCode
                ?: throw AppPackageVersionRequiredException("versionCode")
        }

        validateEnvironment(bundleIdentifier)

        if (appPackageRepository.existsByPlatformAndVersionCode(platform, resolvedVersionCode)) {
            throw AppPackageDuplicateVersionException(resolvedVersionCode)
        }

        val result = storageService.uploadLargePrivate(
            domain = "app-package",
            originalName = file.originalFilename!!,
            bytes = fileBytes,
            contentType = resolveContentType(platform),
        )

        val saved = appPackageRepository.save(
            AppPackage(
                platform = platform,
                versionName = resolvedVersionName,
                versionCode = resolvedVersionCode,
                forceUpdate = forceUpdate,
                releaseNote = releaseNote,
                fileUniqueKey = result.key,
                fileName = file.originalFilename!!,
                fileSize = file.size,
                isLatest = false,
                bundleIdentifier = bundleIdentifier,
                uploadedById = uploadedById,
            )
        )
        val url = storageService.getPresignedUrl(saved.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        appVersionMetaProvider.evict(platform)
        return AppPackageDetailDto.Companion.from(saved, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    /**
     * 최신 지정 — 동일 platform 기존 최신을 해제(1)한 뒤 대상을 최신으로(2). 부분 unique index 정합을 트랜잭션으로 보장.
     */
    @Transactional
    fun setLatest(id: Long): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        appPackageRepository.clearLatest(entity.platform)
        appPackageRepository.flush()
        entity.isLatest = true
        appPackageRepository.save(entity)
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        appVersionMetaProvider.evict(entity.platform)
        return AppPackageDetailDto.Companion.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun setForceUpdate(id: Long, forceUpdate: Boolean): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        entity.forceUpdate = forceUpdate
        appPackageRepository.save(entity)
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        appVersionMetaProvider.evict(entity.platform)
        return AppPackageDetailDto.Companion.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun updateReleaseNote(id: Long, releaseNote: String?): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        entity.releaseNote = releaseNote
        appPackageRepository.save(entity)
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        appVersionMetaProvider.evict(entity.platform)
        return AppPackageDetailDto.Companion.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun delete(id: Long) {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        if (entity.isLatest) throw AppPackageCannotDeleteLatestException()
        storageService.deletePrivate(entity.fileUniqueKey)
        appPackageRepository.delete(entity)
        appVersionMetaProvider.evict(entity.platform)
    }

    private fun validateExtension(platform: AppPlatform, filename: String) {
        val expected = if (platform == AppPlatform.ANDROID) ".apk" else ".ipa"
        if (!filename.lowercase().endsWith(expected)) throw AppPackageInvalidExtensionException(expected)
    }

    /**
     * 서버 stage 와 패키지 빌드 환경(개발/운영)이 일치하는지 검증한다.
     * mobile 빌드는 개발 패키지에만 식별자 `.dev` 접미사를 붙인다
     * (개발 com.otoki.pwrs.mobile.dev / 운영 com.otoki.pwrs.mobile).
     *
     * - prod 서버: 개발(.dev) 패키지 등록 거부
     * - dev  서버: 운영(접미사 없음) 패키지 등록 거부
     * - local 서버: 검증하지 않음 (로컬 개발 편의)
     *
     * 식별자를 추출하지 못한 경우(Android 손상/비APK fallback)는 판별 근거가 없어 통과시킨다
     * (확장자 검증은 [validateExtension] 가 이미 수행).
     */
    private fun validateEnvironment(identifier: String?) {
        if (identifier == null) return
        val isDevPackage = identifier.endsWith(DEV_IDENTIFIER_SUFFIX)
        when (stage.lowercase()) {
            "prod" -> if (isDevPackage) throw AppPackageEnvironmentMismatchException(stage, "개발")
            "dev" -> if (!isDevPackage) throw AppPackageEnvironmentMismatchException(stage, "운영")
            else -> Unit // local 등: 검증하지 않음
        }
    }

    private fun resolveContentType(platform: AppPlatform): String = when (platform) {
        AppPlatform.ANDROID -> "application/vnd.android.package-archive"
        AppPlatform.IOS -> "application/octet-stream"
    }

    companion object {
        /** mobile 빌드가 개발 패키지 식별자(applicationId / bundleIdentifier)에 붙이는 접미사. */
        private const val DEV_IDENTIFIER_SUFFIX = ".dev"
    }
}
