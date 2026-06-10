package com.otoki.powersales.apppackage.service

import com.otoki.powersales.apppackage.dto.AppPackageDetailDto
import com.otoki.powersales.apppackage.dto.AppPackageListItemDto
import com.otoki.powersales.apppackage.entity.AppPackage
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.exception.AppPackageBundleIdentifierRequiredException
import com.otoki.powersales.apppackage.exception.AppPackageCannotDeleteLatestException
import com.otoki.powersales.apppackage.exception.AppPackageDuplicateVersionException
import com.otoki.powersales.apppackage.exception.AppPackageFileRequiredException
import com.otoki.powersales.apppackage.exception.AppPackageInvalidExtensionException
import com.otoki.powersales.apppackage.exception.AppPackageNotFoundException
import com.otoki.powersales.apppackage.repository.AppPackageRepository
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
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
) {

    fun list(platform: AppPlatform?, pageable: Pageable): Page<AppPackageListItemDto> {
        val page = if (platform != null) {
            appPackageRepository.findByPlatform(platform, pageable)
        } else {
            appPackageRepository.findAll(pageable)
        }
        return page.map { AppPackageListItemDto.from(it) }
    }

    fun getDetail(id: Long): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return AppPackageDetailDto.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun upload(
        platform: AppPlatform,
        versionName: String,
        versionCode: Long,
        forceUpdate: Boolean,
        releaseNote: String?,
        file: MultipartFile,
        uploadedById: Long?,
    ): AppPackageDetailDto {
        if (file.isEmpty || file.originalFilename.isNullOrBlank()) throw AppPackageFileRequiredException()
        validateExtension(platform, file.originalFilename!!)
        if (appPackageRepository.existsByPlatformAndVersionCode(platform, versionCode)) {
            throw AppPackageDuplicateVersionException(versionCode)
        }

        val fileBytes = file.bytes
        // iOS 는 서명된 .ipa 내부 Info.plist 의 CFBundleIdentifier 를 자동 추출한다.
        // (수동 입력 시 오타로 OTA manifest 가 불일치해 설치 실패하는 것을 원천 차단)
        val bundleIdentifier = if (platform == AppPlatform.IOS) {
            ipaMetadataExtractor.extract(fileBytes)?.bundleIdentifier
                ?: throw AppPackageBundleIdentifierRequiredException()
        } else {
            null
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
                versionName = versionName,
                versionCode = versionCode,
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
        return AppPackageDetailDto.from(saved, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
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
        return AppPackageDetailDto.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun setForceUpdate(id: Long, forceUpdate: Boolean): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        entity.forceUpdate = forceUpdate
        appPackageRepository.save(entity)
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return AppPackageDetailDto.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun updateReleaseNote(id: Long, releaseNote: String?): AppPackageDetailDto {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        entity.releaseNote = releaseNote
        appPackageRepository.save(entity)
        val url = storageService.getPresignedUrl(entity.fileUniqueKey, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
        return AppPackageDetailDto.from(entity, url, StorageConstants.APP_PACKAGE_PRESIGN_TTL_SECONDS)
    }

    @Transactional
    fun delete(id: Long) {
        val entity = appPackageRepository.findById(id).orElseThrow { AppPackageNotFoundException() }
        if (entity.isLatest) throw AppPackageCannotDeleteLatestException()
        storageService.deletePrivate(entity.fileUniqueKey)
        appPackageRepository.delete(entity)
    }

    private fun validateExtension(platform: AppPlatform, filename: String) {
        val expected = if (platform == AppPlatform.ANDROID) ".apk" else ".ipa"
        if (!filename.lowercase().endsWith(expected)) throw AppPackageInvalidExtensionException(expected)
    }

    private fun resolveContentType(platform: AppPlatform): String = when (platform) {
        AppPlatform.ANDROID -> "application/vnd.android.package-archive"
        AppPlatform.IOS -> "application/octet-stream"
    }
}
