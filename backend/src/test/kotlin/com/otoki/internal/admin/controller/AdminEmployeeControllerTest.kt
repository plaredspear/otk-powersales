package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.EmployeeListItem
import com.otoki.internal.admin.dto.response.EmployeeListResponse
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminEmployeeService
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

@WebMvcTest(AdminEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEmployeeController 테스트")
class AdminEmployeeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminEmployeeService: AdminEmployeeService

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
    @DisplayName("GET /api/v1/admin/employees - 사원 목록 조회")
    inner class GetEmployees {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getEmployees_success() {
            val response = EmployeeListResponse(
                content = listOf(
                    EmployeeListItem(
                        employeeId = "10000001",
                        name = "홍길동",
                        status = "재직",
                        orgName = "서울1지점",
                        costCenterCode = "A001",
                        appAuthority = "조장",
                        startDate = "2020-03-15",
                        appLoginActive = true,
                        workPhone = "010-1234-5678"
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            whenever(adminEmployeeService.getEmployees(eq(1L), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employee_id").value("10000001"))
                .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].status").value("재직"))
                .andExpect(jsonPath("$.data.content[0].org_name").value("서울1지점"))
                .andExpect(jsonPath("$.data.content[0].cost_center_code").value("A001"))
                .andExpect(jsonPath("$.data.content[0].app_authority").value("조장"))
                .andExpect(jsonPath("$.data.content[0].start_date").value("2020-03-15"))
                .andExpect(jsonPath("$.data.content[0].app_login_active").value(true))
                .andExpect(jsonPath("$.data.content[0].work_phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.total_pages").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 전달")
        fun getEmployees_withFilters() {
            val response = EmployeeListResponse(
                content = emptyList(),
                page = 0,
                size = 10,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminEmployeeService.getEmployees(eq(1L), eq("재직"), eq("A001"), eq("홍"), eq("조장"), eq(0), eq(10)))
                .thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/employees")
                    .param("status", "재직")
                    .param("costCenterCode", "A001")
                    .param("keyword", "홍")
                    .param("appAuthority", "조장")
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
        fun getEmployees_empty() {
            val response = EmployeeListResponse(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminEmployeeService.getEmployees(eq(1L), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
                .andExpect(jsonPath("$.data.total_pages").value(0))
        }
    }
}
