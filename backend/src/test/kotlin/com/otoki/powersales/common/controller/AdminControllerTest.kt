package com.otoki.powersales.common.controller

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.auth.service.AuthService
import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminController 테스트")
class AdminControllerTest : MobileControllerTestSupport() {

    @MockkBean
    private lateinit var authService: AuthService

    @Nested
    @DisplayName("POST /api/v1/admin/users/{employeeCode}/reset-device - 단말기 초기화")
    inner class ResetDevice {

        @Test
        @DisplayName("성공 - ADMIN 권한으로 단말기 초기화")
        fun resetDevice_success() {
            authenticateAs(userId = 1L, role = AppAuthority.BRANCH_MANAGER)
            every { authService.resetDevice("20010585") } just Runs

            mockMvc.perform(post("/api/v1/admin/users/20010585/reset-device"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("단말기 등록이 초기화되었습니다"))

            verify { authService.resetDevice("20010585") }
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사번 시 404 USER_NOT_FOUND")
        fun resetDevice_userNotFound() {
            authenticateAs(userId = 1L, role = AppAuthority.BRANCH_MANAGER)
            every { authService.resetDevice("99999999") } throws EmployeeNotFoundException()

            mockMvc.perform(post("/api/v1/admin/users/99999999/reset-device"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }

}
