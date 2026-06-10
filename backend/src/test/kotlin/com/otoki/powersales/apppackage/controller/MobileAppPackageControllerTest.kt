package com.otoki.powersales.apppackage.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.apppackage.dto.AppPackageDownloadUrlDto
import com.otoki.powersales.apppackage.dto.AppVersionCheckDto
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.service.MobileAppPackageService
import com.otoki.powersales.common.test.MobileControllerTestSupport
import io.mockk.every
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MobileAppPackageController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MobileAppPackageController 테스트")
class MobileAppPackageControllerTest : MobileControllerTestSupport() {

    @MockkBean
    private lateinit var mobileAppPackageService: MobileAppPackageService

    @Test
    @DisplayName("버전 체크 — updateAvailable + forceUpdate 응답")
    fun check() {
        every { mobileAppPackageService.check(AppPlatform.ANDROID, 5, any()) } returns
            AppVersionCheckDto(
                platform = AppPlatform.ANDROID,
                updateAvailable = true,
                forceUpdate = true,
                latestVersionName = "1.2.0",
                latestVersionCode = 20,
                releaseNote = "fix",
                downloadUrl = "https://s3/latest.apk",
            )

        mockMvc.perform(get("/api/v1/mobile/app-package/check?platform=ANDROID&versionCode=5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.updateAvailable").value(true))
            .andExpect(jsonPath("$.data.forceUpdate").value(true))
            .andExpect(jsonPath("$.data.downloadUrl").value("https://s3/latest.apk"))
    }

    @Test
    @DisplayName("Android 다운로드 URL 재발급")
    fun downloadUrl() {
        every { mobileAppPackageService.issueDownloadUrl(7L) } returns
            AppPackageDownloadUrlDto(url = "https://s3/app.apk", expiresInSeconds = 900)

        mockMvc.perform(get("/api/v1/mobile/app-package/7/download-url"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.url").value("https://s3/app.apk"))
            .andExpect(jsonPath("$.data.expiresInSeconds").value(900))
    }

    @Test
    @DisplayName("iOS manifest.plist — Content-Type application/xml + XML 본문")
    fun iosManifest() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<plist version="1.0"><dict></dict></plist>"""
        every { mobileAppPackageService.buildIosManifest(7L) } returns xml

        mockMvc.perform(get("/api/v1/mobile/app-package/ios/manifest.plist?id=7"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(xml))
    }

    @Test
    @DisplayName("iOS 설치 안내 페이지 — Content-Type text/html + HTML 본문")
    fun iosInstallPage() {
        val html = "<!DOCTYPE html><html><body>설치</body></html>"
        every { mobileAppPackageService.buildIosInstallPage(7L, any()) } returns html

        mockMvc.perform(get("/api/v1/mobile/app-package/ios/install?id=7"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(html))
    }

    @Test
    @DisplayName("iOS latest manifest — id 없이 최신 버전")
    fun iosLatestManifest() {
        val xml = "<?xml version=\"1.0\"?><plist></plist>"
        every { mobileAppPackageService.buildLatestIosManifest() } returns xml

        mockMvc.perform(get("/api/v1/mobile/app-package/ios/manifest.plist/latest"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(xml))
    }

    @Test
    @DisplayName("iOS latest 설치 페이지 — 고정 링크, id 불필요")
    fun iosLatestInstallPage() {
        val html = "<!DOCTYPE html><html><body>최신 설치</body></html>"
        every { mobileAppPackageService.buildLatestIosInstallPage(any()) } returns html

        mockMvc.perform(get("/api/v1/mobile/app-package/ios/install/latest"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(html))
    }

    @Test
    @DisplayName("X-Forwarded-Proto=https 반영 — TLS 종료 프록시 뒤에서 baseUrl 을 https 로 합성")
    fun forwardedProtoReflectedInBaseUrl() {
        val baseUrlSlot = slot<String>()
        every { mobileAppPackageService.buildLatestIosInstallPage(capture(baseUrlSlot)) } returns
            "<!DOCTYPE html><html><body>설치</body></html>"

        mockMvc.perform(
            get("/api/v1/mobile/app-package/ios/install/latest")
                .header("X-Forwarded-Proto", "https"),
        ).andExpect(status().isOk)

        // http 합성 시 iOS itms-services 가 manifest 를 거부 (인증서 오류로 표시)
        assertThat(baseUrlSlot.captured).isEqualTo("https://localhost")
    }

    @Test
    @DisplayName("X-Forwarded-Proto 부재 시 요청 원본 스킴 유지 — 로컬 직접 접근 동작 불변")
    fun withoutForwardedProtoKeepsRequestScheme() {
        val baseUrlSlot = slot<String>()
        every { mobileAppPackageService.buildLatestIosInstallPage(capture(baseUrlSlot)) } returns
            "<!DOCTYPE html><html><body>설치</body></html>"

        mockMvc.perform(get("/api/v1/mobile/app-package/ios/install/latest"))
            .andExpect(status().isOk)

        assertThat(baseUrlSlot.captured).isEqualTo("http://localhost")
    }

    @Test
    @DisplayName("Android latest 다운로드 — 최신 APK presigned URL 로 302 redirect")
    fun androidLatestDownload() {
        every { mobileAppPackageService.issueLatestAndroidDownloadUrl() } returns
            "https://s3/latest.apk?X-Amz-Signature=abc"

        mockMvc.perform(get("/api/v1/mobile/app-package/android/download/latest"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "https://s3/latest.apk?X-Amz-Signature=abc"))
    }
}
