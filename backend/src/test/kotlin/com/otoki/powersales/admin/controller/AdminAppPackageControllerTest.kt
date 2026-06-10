package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.apppackage.dto.AppPackageDetailDto
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.service.AdminAppPackageService
import com.otoki.powersales.common.test.AdminControllerTestSupport
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminAppPackageController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAppPackageController 테스트")
class AdminAppPackageControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var adminAppPackageService: AdminAppPackageService

    private fun detail(id: Long = 1L, platform: AppPlatform = AppPlatform.ANDROID, isLatest: Boolean = false) =
        AppPackageDetailDto(
            id = id,
            platform = platform,
            versionName = "1.0.0",
            versionCode = 10,
            forceUpdate = false,
            isLatest = isLatest,
            releaseNote = "note",
            fileName = "app.apk",
            fileSize = 1024,
            bundleIdentifier = null,
            downloadUrl = "https://s3/app.apk",
            downloadUrlExpiresInSeconds = 900,
            iosInstallUrl = null,
            uploadedAt = LocalDateTime.of(2026, 6, 10, 12, 0),
        )

    @Test
    @DisplayName("업로드 — multipart 정상 처리 (200 + ApiResponse)")
    fun upload() {
        every {
            adminAppPackageService.upload(any(), any(), any(), any(), any(), any(), any())
        } returns detail()

        val file = MockMultipartFile("file", "app.apk", "application/octet-stream", ByteArray(10))

        mockMvc.perform(
            multipart("/api/v1/admin/app-package")
                .file(file)
                .param("platform", "ANDROID")
                .param("versionName", "1.0.0")
                .param("versionCode", "10")
                .param("forceUpdate", "false")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.versionCode").value(10))
            .andExpect(jsonPath("$.data.downloadUrl").value("https://s3/app.apk"))
            .andExpect(jsonPath("$.message").value("패키지가 업로드되었습니다"))
    }

    @Test
    @DisplayName("최신 지정 — PATCH /{id}/latest")
    fun setLatest() {
        every { adminAppPackageService.setLatest(1L) } returns detail(isLatest = true)

        mockMvc.perform(patch("/api/v1/admin/app-package/1/latest"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isLatest").value(true))
            .andExpect(jsonPath("$.message").value("최신 버전으로 지정되었습니다"))
    }

    @Test
    @DisplayName("강제 토글 — PATCH /{id}/force-update body")
    fun forceUpdate() {
        every { adminAppPackageService.setForceUpdate(1L, true) } returns detail()

        mockMvc.perform(
            patch("/api/v1/admin/app-package/1/force-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"forceUpdate":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
        verify { adminAppPackageService.setForceUpdate(1L, true) }
    }

    @Test
    @DisplayName("삭제 — DELETE /{id}")
    fun delete() {
        every { adminAppPackageService.delete(1L) } returns Unit

        mockMvc.perform(delete("/api/v1/admin/app-package/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("삭제되었습니다"))
        verify { adminAppPackageService.delete(1L) }
    }
}
