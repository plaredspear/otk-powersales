package com.otoki.powersales.apppackage.service

import com.otoki.powersales.apppackage.entity.AppPackage
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.exception.AppPackageBundleIdentifierRequiredException
import com.otoki.powersales.apppackage.exception.AppPackageCannotDeleteLatestException
import com.otoki.powersales.apppackage.exception.AppPackageDuplicateVersionException
import com.otoki.powersales.apppackage.exception.AppPackageFileRequiredException
import com.otoki.powersales.apppackage.exception.AppPackageInvalidExtensionException
import com.otoki.powersales.apppackage.exception.AppPackageNotFoundException
import com.otoki.powersales.apppackage.repository.AppPackageRepository
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

@DisplayName("AppPackage 서비스 테스트")
class AppPackageServiceTest {

    private val repository = mockk<AppPackageRepository>(relaxed = true)
    private val storageService = mockk<StorageService>(relaxed = true)
    private val manifestPlistBuilder = ManifestPlistBuilder()
    private val iosInstallPageBuilder = IosInstallPageBuilder()
    private val ipaMetadataExtractor = IpaMetadataExtractor()
    // APK 추출은 실제 .apk 바이트 생성이 어려워 mock. 기본은 추출 실패(null) → 입력값 fallback.
    private val apkMetadataExtractor = mockk<ApkMetadataExtractor>()

    private val adminService =
        AdminAppPackageService(repository, storageService, ipaMetadataExtractor, apkMetadataExtractor)
    private val mobileService =
        MobileAppPackageService(repository, storageService, manifestPlistBuilder, iosInstallPageBuilder)

    init {
        every { apkMetadataExtractor.extract(any()) } returns null
    }

    /**
     * 테스트용 최소 .ipa(ZIP) 바이트 생성. `Payload/<App>.app/Info.plist` 에
     * 주어진 bundle id 를 담은 XML plist 를 넣는다.
     */
    private fun fakeIpa(
        bundleId: String = "com.otoki.pwrs.mobile",
        shortVersion: String = "1.0.0",
        bundleVersion: String = "1",
    ): ByteArray {
        val plist = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>$bundleId</string>
<key>CFBundleShortVersionString</key><string>$shortVersion</string>
<key>CFBundleVersion</key><string>$bundleVersion</string>
</dict></plist>"""
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("Payload/Runner.app/Info.plist"))
            zos.write(plist.toByteArray())
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun entity(
        id: Long = 1L,
        platform: AppPlatform = AppPlatform.ANDROID,
        versionCode: Long = 10,
        forceUpdate: Boolean = false,
        isLatest: Boolean = false,
        bundleIdentifier: String? = null,
    ) = AppPackage(
        id = id,
        platform = platform,
        versionName = "1.0.$versionCode",
        versionCode = versionCode,
        forceUpdate = forceUpdate,
        releaseNote = "note",
        fileUniqueKey = "uploads/app-package/2026/06/10/abc.apk",
        fileName = "app.apk",
        fileSize = 1024,
        isLatest = isLatest,
        bundleIdentifier = bundleIdentifier,
    )

    @Nested
    @DisplayName("업로드")
    inner class Upload {

        @Test
        @DisplayName("ANDROID 정상 업로드 — uploadLargePrivate 호출 + 저장")
        fun androidSuccess() {
            val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(10))
            every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
            every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
                UploadResult("uploads/app-package/2026/06/10/abc.apk", "application/vnd.android.package-archive", "app.apk", 10)
            every { repository.save(any()) } answers { firstArg<AppPackage>() }
            every { storageService.getPresignedUrl(any(), any()) } returns "https://s3/abc.apk"

            val result = adminService.upload(
                AppPlatform.ANDROID, "1.0.0", 10, false, "note", file, 1L
            )

            assertThat(result.versionCode).isEqualTo(10)
            assertThat(result.downloadUrl).isEqualTo("https://s3/abc.apk")
            verify { storageService.uploadLargePrivate("app-package", "app.apk", any(), "application/vnd.android.package-archive") }
        }

        @Test
        @DisplayName("ANDROID — .apk AndroidManifest 에서 applicationId/versionName/versionCode 자동 추출 (입력 미전달)")
        fun androidAutoExtract() {
            val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(10))
            every { apkMetadataExtractor.extract(any()) } returns
                ApkMetadataExtractor.ApkMetadata("com.otoki.pwrs.mobile", "3.4.5", 345)
            every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
            every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
                UploadResult("uploads/app-package/k.apk", "application/vnd.android.package-archive", "app.apk", 10)
            val saved = slot<AppPackage>()
            every { repository.save(capture(saved)) } answers { firstArg<AppPackage>() }
            every { storageService.getPresignedUrl(any(), any()) } returns "https://s3/abc.apk"

            adminService.upload(AppPlatform.ANDROID, null, null, false, null, file, 1L)

            assertThat(saved.captured.bundleIdentifier).isEqualTo("com.otoki.pwrs.mobile")
            assertThat(saved.captured.versionName).isEqualTo("3.4.5")
            assertThat(saved.captured.versionCode).isEqualTo(345)
        }

        @Test
        @DisplayName("ANDROID — APK 파싱 실패 시 입력 versionName/versionCode 로 fallback")
        fun androidExtractFailFallsBackToInput() {
            val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(10))
            // init 의 기본 stub: apkMetadataExtractor.extract(any()) returns null
            every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
            every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
                UploadResult("uploads/app-package/k.apk", "ct", "app.apk", 10)
            val saved = slot<AppPackage>()
            every { repository.save(capture(saved)) } answers { firstArg<AppPackage>() }
            every { storageService.getPresignedUrl(any(), any()) } returns "url"

            adminService.upload(AppPlatform.ANDROID, "9.9.9", 999, false, null, file, 1L)

            assertThat(saved.captured.bundleIdentifier).isNull()
            assertThat(saved.captured.versionName).isEqualTo("9.9.9")
            assertThat(saved.captured.versionCode).isEqualTo(999)
        }

        @Test
        @DisplayName("IOS 정상 업로드 — .ipa Info.plist 에서 bundleId/versionName/versionCode 자동 추출 (입력 미전달)")
        fun iosSuccess() {
            // shortVersion=2.3.1, bundleVersion=42 인 .ipa. 입력 versionName/versionCode 는 null.
            val file = MockMultipartFile(
                "file", "mobile.ipa", "application/octet-stream",
                fakeIpa("com.otoki.pwrs.mobile", shortVersion = "2.3.1", bundleVersion = "42"),
            )
            every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
            every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
                UploadResult("uploads/app-package/2026/06/10/abc.ipa", "application/octet-stream", "mobile.ipa", 10)
            val saved = slot<AppPackage>()
            every { repository.save(capture(saved)) } answers { firstArg<AppPackage>() }
            every { storageService.getPresignedUrl(any(), any()) } returns "https://s3/abc.ipa"

            val result = adminService.upload(
                AppPlatform.IOS, null, null, false, null, file, 1L
            )

            assertThat(saved.captured.bundleIdentifier).isEqualTo("com.otoki.pwrs.mobile")
            assertThat(saved.captured.versionName).isEqualTo("2.3.1")
            assertThat(saved.captured.versionCode).isEqualTo(42)
            assertThat(result.bundleIdentifier).isEqualTo("com.otoki.pwrs.mobile")
        }

        @Test
        @DisplayName("IOS CFBundleVersion 이 비정수(1.2.3)면 입력 versionCode 로 fallback")
        fun iosNonIntegerBundleVersionFallsBackToInput() {
            val file = MockMultipartFile(
                "file", "mobile.ipa", "application/octet-stream",
                fakeIpa("com.otoki.pwrs.mobile", shortVersion = "1.2.3", bundleVersion = "1.2.3"),
            )
            every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
            every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
                UploadResult("uploads/app-package/k.ipa", "ct", "mobile.ipa", 10)
            val saved = slot<AppPackage>()
            every { repository.save(capture(saved)) } answers { firstArg<AppPackage>() }
            every { storageService.getPresignedUrl(any(), any()) } returns "url"

            // bundleVersion=1.2.3 은 Long 변환 불가 → 입력 99 로 fallback
            adminService.upload(AppPlatform.IOS, null, 99, false, null, file, 1L)

            assertThat(saved.captured.versionName).isEqualTo("1.2.3")
            assertThat(saved.captured.versionCode).isEqualTo(99)
        }

        @Test
        @DisplayName("빈 파일 → AppPackageFileRequiredException")
        fun emptyFile() {
            val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(0))
            assertThatThrownBy {
                adminService.upload(AppPlatform.ANDROID, "1.0.0", 10, false, null, file, 1L)
            }.isInstanceOf(AppPackageFileRequiredException::class.java)
        }

        @Test
        @DisplayName("확장자 불일치 (ANDROID 에 .ipa) → AppPackageInvalidExtensionException")
        fun wrongExtension() {
            val file = MockMultipartFile("file", "app.ipa", "application/octet-stream", ByteArray(10))
            assertThatThrownBy {
                adminService.upload(AppPlatform.ANDROID, "1.0.0", 10, false, null, file, 1L)
            }.isInstanceOf(AppPackageInvalidExtensionException::class.java)
        }

        @Test
        @DisplayName("IOS 인데 .ipa 에서 bundleIdentifier 추출 불가 → AppPackageBundleIdentifierRequiredException")
        fun iosBundleIdNotExtractable() {
            // Info.plist 가 없는 손상된/위조 .ipa (단순 바이트)
            every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
            val file = MockMultipartFile("file", "app.ipa", "application/octet-stream", ByteArray(10))
            assertThatThrownBy {
                adminService.upload(AppPlatform.IOS, "1.0.0", 10, false, null, file, 1L)
            }.isInstanceOf(AppPackageBundleIdentifierRequiredException::class.java)
        }

        @Test
        @DisplayName("중복 versionCode → AppPackageDuplicateVersionException")
        fun duplicateVersion() {
            val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(10))
            every { repository.existsByPlatformAndVersionCode(AppPlatform.ANDROID, 10) } returns true
            assertThatThrownBy {
                adminService.upload(AppPlatform.ANDROID, "1.0.0", 10, false, null, file, 1L)
            }.isInstanceOf(AppPackageDuplicateVersionException::class.java)
        }
    }

    @Nested
    @DisplayName("최신 지정")
    inner class SetLatest {

        @Test
        @DisplayName("기존 최신 해제 후 대상 지정 — clearLatest 가 isLatest=true 보다 먼저")
        fun clearThenSet() {
            val target = entity(id = 2L, isLatest = false)
            every { repository.findById(2L) } returns Optional.of(target)
            every { repository.save(any()) } answers { firstArg<AppPackage>() }
            every { storageService.getPresignedUrl(any(), any()) } returns "url"

            val result = adminService.setLatest(2L)

            assertThat(result.isLatest).isTrue()
            verifyOrder {
                repository.clearLatest(AppPlatform.ANDROID)
                repository.save(target)
            }
        }
    }

    @Nested
    @DisplayName("삭제")
    inner class Delete {

        @Test
        @DisplayName("최신 지정 버전 삭제 거부 → AppPackageCannotDeleteLatestException")
        fun cannotDeleteLatest() {
            every { repository.findById(1L) } returns Optional.of(entity(isLatest = true))
            assertThatThrownBy { adminService.delete(1L) }
                .isInstanceOf(AppPackageCannotDeleteLatestException::class.java)
            verify(exactly = 0) { storageService.deletePrivate(any()) }
        }

        @Test
        @DisplayName("일반 버전 삭제 — S3 deletePrivate + row 삭제")
        fun deleteNonLatest() {
            val e = entity(isLatest = false)
            every { repository.findById(1L) } returns Optional.of(e)
            adminService.delete(1L)
            verify { storageService.deletePrivate(e.fileUniqueKey) }
            verify { repository.delete(e) }
        }

        @Test
        @DisplayName("미존재 → AppPackageNotFoundException")
        fun notFound() {
            every { repository.findById(99L) } returns Optional.empty()
            assertThatThrownBy { adminService.delete(99L) }
                .isInstanceOf(AppPackageNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("버전 체크")
    inner class VersionCheck {

        @Test
        @DisplayName("최신 부재 → updateAvailable=false")
        fun noLatest() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns null
            every { repository.findTopByPlatformOrderByVersionCodeDesc(AppPlatform.ANDROID) } returns null

            val result = mobileService.check(AppPlatform.ANDROID, 5, "https://api.example.com")

            assertThat(result.updateAvailable).isFalse()
            assertThat(result.downloadUrl).isNull()
        }

        @Test
        @DisplayName("최신 > 요청 + 강제버전 존재 → updateAvailable + forceUpdate (Android presigned URL)")
        fun androidForceUpdate() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns entity(versionCode = 20, isLatest = true)
            every {
                repository.existsByPlatformAndForceUpdateTrueAndVersionCodeGreaterThan(AppPlatform.ANDROID, 5)
            } returns true
            every { storageService.getPresignedUrl(any(), any()) } returns "https://s3/latest.apk"

            val result = mobileService.check(AppPlatform.ANDROID, 5, "https://api.example.com")

            assertThat(result.updateAvailable).isTrue()
            assertThat(result.forceUpdate).isTrue()
            assertThat(result.downloadUrl).isEqualTo("https://s3/latest.apk")
        }

        @Test
        @DisplayName("최신 = 요청 → updateAvailable=false (forceUpdate 판정 안 함)")
        fun upToDate() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.ANDROID) } returns entity(versionCode = 10, isLatest = true)

            val result = mobileService.check(AppPlatform.ANDROID, 10, "https://api.example.com")

            assertThat(result.updateAvailable).isFalse()
            assertThat(result.forceUpdate).isFalse()
        }

        @Test
        @DisplayName("iOS 다운로드 URL = itms-services manifest 엔드포인트 (URL 인코딩)")
        fun iosDownloadUrl() {
            every { repository.findByPlatformAndIsLatestTrue(AppPlatform.IOS) } returns
                entity(id = 7L, platform = AppPlatform.IOS, versionCode = 20, isLatest = true, bundleIdentifier = "com.otoki.app")
            every {
                repository.existsByPlatformAndForceUpdateTrueAndVersionCodeGreaterThan(AppPlatform.IOS, 5)
            } returns false

            val result = mobileService.check(AppPlatform.IOS, 5, "https://api.example.com")

            assertThat(result.downloadUrl).startsWith("itms-services://?action=download-manifest&url=")
            // manifest 엔드포인트 절대 URL 이 URL 인코딩되어 포함
            assertThat(result.downloadUrl).contains("api.example.com")
            assertThat(result.downloadUrl).contains("manifest.plist")
            assertThat(result.downloadUrl).contains("id%3D7")
        }
    }

    @Nested
    @DisplayName("iOS manifest.plist 생성")
    inner class IosManifest {

        @Test
        @DisplayName("presigned URL 의 & 를 &amp; 로 XML 이스케이프 + 메타 주입")
        fun manifestEscapesAmpersand() {
            every { repository.findById(7L) } returns Optional.of(
                entity(id = 7L, platform = AppPlatform.IOS, isLatest = true, bundleIdentifier = "com.otoki.app")
            )
            // presigned URL 은 보통 &X-Amz-... 쿼리를 포함
            every { storageService.getPresignedUrl(any(), any()) } returns
                "https://s3/app.ipa?X-Amz-Signature=abc&X-Amz-Date=20260610"

            val xml = mobileService.buildIosManifest(7L)

            assertThat(xml).contains("&amp;X-Amz-Date")
            assertThat(xml).doesNotContain("Signature=abc&X-Amz-Date") // raw & 가 남으면 안 됨
            assertThat(xml).contains("<key>bundle-identifier</key><string>com.otoki.app</string>")
            assertThat(xml).contains("software-package")
        }
    }

    @Test
    @DisplayName("uploadedById 가 엔티티에 저장됨")
    fun uploadedByPersisted() {
        val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(10))
        every { repository.existsByPlatformAndVersionCode(any(), any()) } returns false
        every { storageService.uploadLargePrivate(any(), any(), any(), any()) } returns
            UploadResult("uploads/app-package/k.apk", "ct", "app.apk", 10)
        val saved = slot<AppPackage>()
        every { repository.save(capture(saved)) } answers { firstArg<AppPackage>() }
        every { storageService.getPresignedUrl(any(), any()) } returns "url"

        adminService.upload(AppPlatform.ANDROID, "1.0.0", 10, false, null, file, 42L)

        assertThat(saved.captured.uploadedById).isEqualTo(42L)
    }
}
