package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.employee.dto.response.EmployeeListItem
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.service.AdminEmployeeService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
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
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockitoBean
    private lateinit var dataScopeHolder: DataScopeHolder

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.BRANCH_MANAGER)
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
                        id = 1L,
                        employeeCode = "10000001",
                        name = "홍길동",
                        status = "재직",
                        gender = "남",
                        orgName = "서울1지점",
                        costCenterCode = "A001",
                        role = "LEADER",
                        roleLabel = "조장",
                        startDate = "2020-03-15",
                        endDate = null,
                        appLoginActive = true,
                        workPhone = "010-1234-5678",
                        jikchak = null,
                        jikwee = null,
                        jikgub = null,
                        jobCode = null,
                        appointmentDate = null,
                        ordDetailNode = null
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            whenever(adminEmployeeService.getEmployees(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("10000001"))
                .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].status").value("재직"))
                .andExpect(jsonPath("$.data.content[0].gender").value("남"))
                .andExpect(jsonPath("$.data.content[0].orgName").value("서울1지점"))
                .andExpect(jsonPath("$.data.content[0].costCenterCode").value("A001"))
                .andExpect(jsonPath("$.data.content[0].role").value("LEADER"))
                .andExpect(jsonPath("$.data.content[0].roleLabel").value("조장"))
                .andExpect(jsonPath("$.data.content[0].startDate").value("2020-03-15"))
                .andExpect(jsonPath("$.data.content[0].appLoginActive").value(true))
                .andExpect(jsonPath("$.data.content[0].workPhone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
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
            whenever(adminEmployeeService.getEmployees(eq("재직"), eq("A001"), eq("홍"), eq(UserRole.LEADER), eq(0), eq(10)))
                .thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/employees")
                    .param("status", "재직")
                    .param("costCenterCode", "A001")
                    .param("keyword", "홍")
                    .param("role", "LEADER")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
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
            whenever(adminEmployeeService.getEmployees(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
        }
    }
}
