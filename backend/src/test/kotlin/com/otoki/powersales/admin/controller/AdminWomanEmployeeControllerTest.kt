package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.employee.dto.response.EmployeeListItem
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.service.AdminEmployeeService
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.user.entity.ProfileType
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminWomanEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminWomanEmployeeController 테스트")
class AdminWomanEmployeeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var adminEmployeeService: AdminEmployeeService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRoleEnum.BRANCH_MANAGER,
            costCenterCode = null,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true,
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Test
    @DisplayName("GET /api/v1/admin/women-employees - role 파라미터 없이도 WOMAN 으로 강제 조회")
    fun getWomanEmployees_forcesWomanRole() {
        val response = EmployeeListResponse(
            content = listOf(
                EmployeeListItem(
                    id = 1L,
                    employeeCode = "10000001",
                    name = "김여사",
                    status = "재직",
                    gender = "여",
                    orgName = "서울1지점",
                    costCenterCode = "A001",
                    role = "WOMAN",
                    roleLabel = "여사원",
                    startDate = "2020-03-15",
                    endDate = null,
                    appLoginActive = true,
                    workPhone = null,
                    jikchak = null,
                    jikwee = null,
                    jikgub = null,
                    jobCode = null,
                    appointmentDate = null,
                    ordDetailNode = null,
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
        )
        every {
            adminEmployeeService.getEmployees(any(), any(), any(), any(), eq(UserRoleEnum.WOMAN), any(), any())
        } returns response

        mockMvc.perform(get("/api/v1/admin/women-employees"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].role").value("WOMAN"))
            .andExpect(jsonPath("$.data.content[0].roleLabel").value("여사원"))

        verify(exactly = 1) {
            adminEmployeeService.getEmployees(any(), any(), any(), any(), eq(UserRoleEnum.WOMAN), any(), any())
        }
    }

    @Test
    @DisplayName("필터 파라미터 (status/costCenterCode/keyword/page/size) 전달")
    fun getWomanEmployees_withFilters() {
        val response = EmployeeListResponse(
            content = emptyList(),
            page = 0,
            size = 10,
            totalElements = 0,
            totalPages = 0,
        )
        every {
            adminEmployeeService.getEmployees(any(), eq("재직"), eq("A001"), eq("김"), eq(UserRoleEnum.WOMAN), eq(0), eq(10))
        } returns response

        mockMvc.perform(
            get("/api/v1/admin/women-employees")
                .param("status", "재직")
                .param("costCenterCode", "A001")
                .param("keyword", "김")
                .param("page", "0")
                .param("size", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }
}
