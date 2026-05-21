package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.employee.dto.response.EmployeeListItem
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.service.AdminEmployeeService
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminWomanEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminWomanEmployeeController 테스트")
class AdminWomanEmployeeControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var adminEmployeeService: AdminEmployeeService

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
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
