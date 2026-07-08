package com.otoki.powersales.admin.tools.logging

import com.otoki.powersales.admin.tools.logging.controller.LogLevelController
import com.otoki.powersales.admin.tools.logging.dto.LoggerLevelResponse
import com.otoki.powersales.admin.tools.logging.dto.LoggerListResponse
import com.otoki.powersales.admin.tools.logging.service.LogLevelService
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * LogLevelController 권한 가드 검증.
 *
 * 핵심 보안 로직인 `requireSystemAdmin` 이 어노테이션이 아니라 명령형 호출이라, 회귀 방어를 위해
 * (a) 비-시스템관리자 → 403, (b) 시스템관리자 → 200 을 명시 검증한다. addFilters=false 로
 * WebAdminContextFilter 를 배제하므로, 컨트롤러 자체 가드만 단독으로 시험한다.
 */
@WebMvcTest(LogLevelController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LogLevelController 권한 가드 테스트")
class LogLevelControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var logLevelService: LogLevelService

    @Test
    @DisplayName("GET - 시스템 관리자는 200 + 로거 목록 반환")
    fun getLoggers_systemAdmin_ok() {
        authenticateAsAdmin(role = null, profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
        every { logLevelService.getLoggers() } returns LoggerListResponse(
            availableLevels = listOf("INFO", "DEBUG"),
            loggers = listOf(LoggerLevelResponse("ROOT", "INFO", "INFO")),
        )

        mockMvc.perform(get("/api/v1/admin/tools/log-levels"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.availableLevels[0]").value("INFO"))
            .andExpect(jsonPath("$.data.loggers[0].name").value("ROOT"))
    }

    @Test
    @DisplayName("GET - 비 시스템 관리자는 403")
    fun getLoggers_nonAdmin_forbidden() {
        authenticateAsAdmin(role = null, profileName = "9. Staff")

        mockMvc.perform(get("/api/v1/admin/tools/log-levels"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))

        verify(exactly = 0) { logLevelService.getLoggers() }
    }

    @Test
    @DisplayName("POST - 시스템 관리자는 레벨 변경 200")
    fun updateLevel_systemAdmin_ok() {
        authenticateAsAdmin(role = null, profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
        every { logLevelService.setLevel("com.otoki.powersales", "DEBUG") } returns
            LoggerLevelResponse("com.otoki.powersales", "DEBUG", "DEBUG")

        mockMvc.perform(
            post("/api/v1/admin/tools/log-levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loggerName":"com.otoki.powersales","level":"DEBUG"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.configuredLevel").value("DEBUG"))
    }

    @Test
    @DisplayName("POST - 비 시스템 관리자는 403 (서비스 미호출)")
    fun updateLevel_nonAdmin_forbidden() {
        authenticateAsAdmin(role = null, profileName = "9. Staff")

        mockMvc.perform(
            post("/api/v1/admin/tools/log-levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loggerName":"com.otoki.powersales","level":"DEBUG"}"""),
        )
            .andExpect(status().isForbidden)

        verify(exactly = 0) { logLevelService.setLevel(any(), any()) }
    }

    @Test
    @DisplayName("POST - loggerName 누락 body 는 400 (@Valid)")
    fun updateLevel_blankLoggerName_badRequest() {
        authenticateAsAdmin(role = null, profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)

        mockMvc.perform(
            post("/api/v1/admin/tools/log-levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loggerName":"","level":"DEBUG"}"""),
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { logLevelService.setLevel(any(), any()) }
    }
}
