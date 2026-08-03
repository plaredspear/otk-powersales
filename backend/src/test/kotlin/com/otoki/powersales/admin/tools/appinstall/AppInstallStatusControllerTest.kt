package com.otoki.powersales.admin.tools.appinstall

import com.otoki.powersales.admin.tools.appinstall.controller.AppInstallStatusController
import com.otoki.powersales.domain.org.employee.dto.response.AppUninstalledFemaleStaffSummary
import com.otoki.powersales.domain.org.employee.service.EmployeeAppInstallService
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * AppInstallStatusController 권한 가드 + 응답 검증.
 *
 * 사원 명단(사번/이름/소속) 을 통째로 내려주는 엔드포인트라 `requireSystemAdmin` 회귀 방어가 핵심이다.
 * 가드가 어노테이션이 아닌 명령형 호출이므로 (a) 비-시스템관리자 403, (b) 시스템관리자 200 을 명시 검증한다.
 */
@WebMvcTest(AppInstallStatusController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AppInstallStatusController 테스트")
class AppInstallStatusControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var employeeAppInstallService: EmployeeAppInstallService

    @Test
    @DisplayName("집계 조회 - 시스템 관리자는 200 + 미설치 인원/모수 반환")
    fun summary_systemAdmin_ok() {
        authenticateAsAdmin(role = null, profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
        every { employeeAppInstallService.getUninstalledFemaleStaffSummary() } returns
            AppUninstalledFemaleStaffSummary(uninstalledCount = 7, targetCount = 120L)

        mockMvc.perform(get("/api/v1/admin/tools/app-install/uninstalled-female-staff"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.uninstalledCount").value(7))
            .andExpect(jsonPath("$.data.targetCount").value(120))
    }

    @Test
    @DisplayName("집계 조회 - 비 시스템 관리자는 403 (서비스 미호출)")
    fun summary_nonAdmin_forbidden() {
        authenticateAsAdmin(role = null, profileName = "9. Staff")

        mockMvc.perform(get("/api/v1/admin/tools/app-install/uninstalled-female-staff"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))

        verify(exactly = 0) { employeeAppInstallService.getUninstalledFemaleStaffSummary() }
    }

    @Test
    @DisplayName("엑셀 다운로드 - 시스템 관리자는 200 + 한글 파일명 Content-Disposition")
    fun export_systemAdmin_ok() {
        authenticateAsAdmin(role = null, profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
        every { employeeAppInstallService.exportUninstalledFemaleStaff() } returns
            ExcelResult(byteArrayOf(1, 2, 3), "앱미설치여사원_20260803.xlsx")

        mockMvc.perform(get("/api/v1/admin/tools/app-install/uninstalled-female-staff/export"))
            .andExpect(status().isOk)
            // RFC 5987 인코딩 — 컨트롤러가 ExcelResponseUtils 로 붙이는 헤더라 verbatim 확인.
            .andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.containsString("filename*=UTF-8''"),
                ),
            )
    }

    @Test
    @DisplayName("엑셀 다운로드 - 비 시스템 관리자는 403 (서비스 미호출)")
    fun export_nonAdmin_forbidden() {
        authenticateAsAdmin(role = null, profileName = "9. Staff")

        mockMvc.perform(get("/api/v1/admin/tools/app-install/uninstalled-female-staff/export"))
            .andExpect(status().isForbidden)

        verify(exactly = 0) { employeeAppInstallService.exportUninstalledFemaleStaff() }
    }
}
