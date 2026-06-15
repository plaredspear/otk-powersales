package com.otoki.powersales.platform.apppackage.service

import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.apppackage.exception.AppPackageEnvironmentMismatchException
import com.otoki.powersales.platform.apppackage.repository.AppPackageRepository
import com.otoki.powersales.platform.apppackage.entity.AppPackage
import com.otoki.powersales.platform.common.config.DomainProperties
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile

/**
 * AdminAppPackageService 의 서버 환경(stage) ↔ 패키지 환경(개발/운영) 정합 검증 단위 테스트.
 *
 * mobile 빌드는 개발 패키지 식별자에만 `.dev` 접미사를 붙인다
 * (개발 com.otoki.pwrs.mobile.dev / 운영 com.otoki.pwrs.mobile).
 *
 * IPA 경로로 테스트한다(추출기가 bundleIdentifier 를 반드시 반환 — null fallback 없음).
 * 환경 검증은 중복 버전 체크/저장보다 앞서 실행되므로, 거부 케이스는 추출기 mock 만으로 도달한다.
 */
@DisplayName("AdminAppPackageService — 개발/운영 패키지 환경 정합")
class AdminAppPackageServiceEnvironmentTest {

    private val appPackageRepository = mockk<AppPackageRepository>(relaxed = true)
    private val storageService = mockk<StorageService>(relaxed = true)
    private val apkMetadataExtractor = mockk<ApkMetadataExtractor>()
    private val appVersionMetaProvider = mockk<AppVersionMetaProvider>(relaxed = true)

    private val PROD_ID = "com.otoki.pwrs.mobile"
    private val DEV_ID = "com.otoki.pwrs.mobile.dev"

    private fun service(stage: String, ipaBundleId: String): AdminAppPackageService {
        val ipaMetadataExtractor = mockk<IpaMetadataExtractor>()
        every { ipaMetadataExtractor.extract(any()) } returns
            IpaMetadataExtractor.IpaMetadata(bundleIdentifier = ipaBundleId, shortVersion = "1.0.0", bundleVersion = "100")
        return AdminAppPackageService(
            appPackageRepository = appPackageRepository,
            storageService = storageService,
            ipaMetadataExtractor = ipaMetadataExtractor,
            apkMetadataExtractor = apkMetadataExtractor,
            domainProperties = DomainProperties(),
            appVersionMetaProvider = appVersionMetaProvider,
            stage = stage,
        )
    }

    private fun ipaFile(): MultipartFile {
        val file = mockk<MultipartFile>()
        every { file.isEmpty } returns false
        every { file.originalFilename } returns "app.ipa"
        every { file.bytes } returns ByteArray(1)
        every { file.size } returns 1L
        return file
    }

    private fun upload(stage: String, bundleId: String): Any {
        // 환경 검증을 통과하는 케이스가 저장 단계까지 진행될 때 필요한 stub.
        // (거부 케이스는 검증이 먼저 예외를 던져 여기에 도달하지 않으므로 무해)
        every { appPackageRepository.existsByPlatformAndVersionCode(any(), any()) } returns false
        every { appPackageRepository.save(any<AppPackage>()) } answers { firstArg() }
        every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
            UploadResult(key = "k", contentType = "c", originalName = "app.ipa", sizeBytes = 1L)
        every { storageService.getPresignedUrl(any(), any()) } returns "https://example/url"
        return service(stage, bundleId).upload(
            platform = AppPlatform.IOS,
            versionName = null,
            versionCode = null,
            forceUpdate = false,
            releaseNote = null,
            file = ipaFile(),
            uploadedById = 1L,
        )
    }

    @Test
    @DisplayName("prod 서버에 개발(.dev) 패키지 → 거부")
    fun prodRejectsDevPackage() {
        assertThatThrownBy { upload("prod", DEV_ID) }
            .isInstanceOf(AppPackageEnvironmentMismatchException::class.java)
    }

    @Test
    @DisplayName("dev 서버에 운영(접미사 없음) 패키지 → 거부")
    fun devRejectsProdPackage() {
        assertThatThrownBy { upload("dev", PROD_ID) }
            .isInstanceOf(AppPackageEnvironmentMismatchException::class.java)
    }

    @Test
    @DisplayName("prod 서버에 운영 패키지 → 환경 검증 통과 (정상 저장)")
    fun prodAcceptsProdPackage() {
        assertThatCode { upload("prod", PROD_ID) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("dev 서버에 개발 패키지 → 환경 검증 통과 (정상 저장)")
    fun devAcceptsDevPackage() {
        assertThatCode { upload("dev", DEV_ID) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("local 서버 → 환경 검증 스킵 (개발/운영 패키지 모두 통과)")
    fun localSkipsValidation() {
        assertThatCode { upload("local", PROD_ID) }.doesNotThrowAnyException()
        assertThatCode { upload("local", DEV_ID) }.doesNotThrowAnyException()
    }
}
