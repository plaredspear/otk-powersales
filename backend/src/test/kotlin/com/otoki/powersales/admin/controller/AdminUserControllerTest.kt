package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.AdminUserDetailResponse
import com.otoki.powersales.admin.dto.AdminUserListItem
import com.otoki.powersales.admin.dto.AdminUserListResponse
import com.otoki.powersales.admin.dto.AdminUserPasswordResetResponse
import com.otoki.powersales.admin.dto.UpdateUserActiveStatusRequest
import com.otoki.powersales.admin.exception.AdminUserNotFoundException
import com.otoki.powersales.admin.exception.CannotDeactivateSelfException
import com.otoki.powersales.admin.service.AdminUserService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.user.entity.ProfileType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AdminUserController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminUserController 테스트")
class AdminUserControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminUserService: AdminUserService
    @MockkBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockkBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockkBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockkBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "admin@otokims.co.kr",
            employeeCode = "ADMIN-OPS01",
            employeeId = 1L,
            role = UserRole.SYSTEM_ADMIN,
            costCenterCode = null,
            profileType = ProfileType.SYSTEM_ADMIN,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users - 사용자 목록 조회")
    inner class GetUsers {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getUsers_success() {
            val response = AdminUserListResponse(
                content = listOf(
                    AdminUserListItem(
                        id = 1L,
                        username = "kim@otokims.co.kr",
                        employeeCode = "10000001",
                        name = "김영업",
                        email = "kim@otokims.co.kr",
                        profileType = "SALES_REP",
                        profileTypeLabel = "영업사원",
                        branch = "서울1지점",
                        department = "영업1부",
                        isActive = true,
                        lastLoginAt = java.time.LocalDateTime.of(2026, 5, 14, 10, 0)
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            every { adminUserService.findUsers(any(), any(), eq(0), eq(20)) } returns response

            mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("kim@otokims.co.kr"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("성공 - keyword + isActive 필터")
        fun getUsers_withFilters() {
            val response = AdminUserListResponse(
                content = emptyList(),
                page = 0, size = 10, totalElements = 0, totalPages = 0
            )
            every { adminUserService.findUsers(eq("kim"), eq(false), eq(0), eq(10)) } returns response

            mockMvc.perform(
                get("/api/v1/admin/users")
                    .param("keyword", "kim")
                    .param("isActive", "false")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId} - 사용자 상세 조회")
    inner class GetUser {

        @Test
        @DisplayName("성공 - 200 OK")
        fun getUser_success() {
            val response = AdminUserDetailResponse(
                id = 1L,
                username = "kim@otokims.co.kr",
                employeeCode = "10000001",
                name = "김영업",
                firstName = "영업",
                lastName = "김",
                email = "kim@otokims.co.kr",
                alias = "kim",
                title = "사원",
                department = "영업1부",
                division = "영업본부",
                branch = "서울1지점",
                mobilePhone = "010-1234-5678",
                phone = null,
                hrCode = "HR001",
                sfid = "0058A000001abcdQAA",
                profileType = "SALES_REP",
                profileTypeLabel = "영업사원",
                isSalesSupport = false,
                isActive = true,
                passwordChangeRequired = false,
                lastLoginAt = java.time.LocalDateTime.of(2026, 5, 14, 10, 0),
                createdAt = java.time.LocalDateTime.of(2026, 1, 1, 0, 0),
                lastModifiedAt = java.time.LocalDateTime.of(2026, 5, 14, 10, 0)
            )
            every { adminUserService.findUserDetail(eq(1L)) } returns response

            mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("kim@otokims.co.kr"))
        }

        @Test
        @DisplayName("실패 - 미존재 사용자 → 404")
        fun getUser_notFound() {
            every { adminUserService.findUserDetail(eq(99999L)) } throws AdminUserNotFoundException(99999L)

            mockMvc.perform(get("/api/v1/admin/users/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users/{userId}/reset-password - 비밀번호 임시 리셋")
    inner class ResetPassword {

        @Test
        @DisplayName("성공 - 200 OK, passwordChangeRequired=true")
        fun resetPassword_success() {
            val response = AdminUserPasswordResetResponse(
                userId = 5L,
                username = "kim@otokims.co.kr",
                temporaryPasswordIssued = true,
                passwordChangeRequired = true,
                resetAt = java.time.LocalDateTime.of(2026, 5, 14, 14, 30)
            )
            every { adminUserService.resetPassword(eq(5L)) } returns response

            mockMvc.perform(post("/api/v1/admin/users/5/reset-password"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.userId").value(5L))
                .andExpect(jsonPath("$.message").value("비밀번호가 초기화되었습니다"))
        }

        @Test
        @DisplayName("실패 - 미존재 사용자 → 404")
        fun resetPassword_notFound() {
            every { adminUserService.resetPassword(eq(99999L)) } throws AdminUserNotFoundException(99999L)

            mockMvc.perform(post("/api/v1/admin/users/99999/reset-password"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/active - 활성/비활성 토글")
    inner class UpdateActiveStatus {

        // controller 후처리: isActive 분기로 메시지 한글 다름 ("비활성화" vs "활성화") — 가드레일 5.3 으로 verbatim 유지
        @Test
        @DisplayName("성공 - 비활성화")
        fun deactivate_success() {
            val request = UpdateUserActiveStatusRequest(isActive = false)
            every { adminUserService.updateActiveStatus(any(), any(), any()) } just Runs

            mockMvc.perform(
                put("/api/v1/admin/users/5/active")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("사용자가 비활성화되었습니다"))
        }

        @Test
        @DisplayName("성공 - 활성화")
        fun activate_success() {
            val request = UpdateUserActiveStatusRequest(isActive = true)
            every { adminUserService.updateActiveStatus(any(), any(), any()) } just Runs

            mockMvc.perform(
                put("/api/v1/admin/users/5/active")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("사용자가 활성화되었습니다"))
        }

        @Test
        @DisplayName("실패 - 자기 자신 비활성화 → 400")
        fun deactivateSelf_blocked() {
            val request = UpdateUserActiveStatusRequest(isActive = false)
            every { adminUserService.updateActiveStatus(eq(100L), eq(100L), eq(false)) } throws CannotDeactivateSelfException()

            mockMvc.perform(
                put("/api/v1/admin/users/100/active")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("CANNOT_DEACTIVATE_SELF"))
        }

        @Test
        @DisplayName("실패 - 미존재 사용자 → 404")
        fun update_notFound() {
            val request = UpdateUserActiveStatusRequest(isActive = true)
            every { adminUserService.updateActiveStatus(any(), any(), any()) } throws AdminUserNotFoundException(99999L)

            mockMvc.perform(
                put("/api/v1/admin/users/99999/active")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }
}
