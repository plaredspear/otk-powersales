package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.AccountListItem
import com.otoki.internal.admin.dto.response.AccountListResponse
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminAccountService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminAccountController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAccountController 테스트")
class AdminAccountControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminAccountService: AdminAccountService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts - 거래처 목록 조회")
    inner class GetAccounts {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getAccounts_success() {
            val response = AccountListResponse(
                content = listOf(
                    AccountListItem(
                        externalKey = "AC001234",
                        name = "GS25 역삼점",
                        abcType = "편의점",
                        branchCode = "A001",
                        branchName = "서울1지점",
                        employeeCode = "123456",
                        address1 = "서울시 강남구 역삼동 123-4",
                        phone = "02-1234-5678",
                        accountStatusName = "활성"
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            whenever(adminAccountService.getAccounts(eq(1L), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].external_key").value("AC001234"))
                .andExpect(jsonPath("$.data.content[0].name").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.content[0].abc_type").value("편의점"))
                .andExpect(jsonPath("$.data.content[0].branch_code").value("A001"))
                .andExpect(jsonPath("$.data.content[0].branch_name").value("서울1지점"))
                .andExpect(jsonPath("$.data.content[0].employee_code").value("123456"))
                .andExpect(jsonPath("$.data.content[0].address1").value("서울시 강남구 역삼동 123-4"))
                .andExpect(jsonPath("$.data.content[0].phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.data.content[0].account_status_name").value("활성"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.total_pages").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 전달")
        fun getAccounts_withFilters() {
            val response = AccountListResponse(
                content = emptyList(),
                page = 0,
                size = 10,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminAccountService.getAccounts(eq(1L), eq("GS25"), eq("편의점"), eq("A001"), eq("활성"), eq(0), eq(10)))
                .thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/accounts")
                    .param("keyword", "GS25")
                    .param("abcType", "편의점")
                    .param("branchCode", "A001")
                    .param("accountStatusName", "활성")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getAccounts_empty() {
            val response = AccountListResponse(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminAccountService.getAccounts(eq(1L), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
                .andExpect(jsonPath("$.data.total_pages").value(0))
        }
    }
}
