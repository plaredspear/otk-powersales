package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
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

@WebMvcTest(AdminFemaleEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminFemaleEmployeeController 테스트")
class AdminFemaleEmployeeControllerTest : AdminControllerTestSupport() {

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
    @DisplayName("GET /api/v1/admin/female-employees - role 파라미터 없이도 WOMAN 으로 강제 조회")
    fun getFemaleEmployees_forcesWomanRole() {
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
                    role = "여사원",
                    
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
                    jikjong = "OSPM",
                    workEmail = "kim@otoki.com",
                    phone = "01012345678",
                    age = "45살",
                    yearsOfService = "5년",
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
        )
        every {
            adminEmployeeService.getEmployees(
                any(), any(), any(), any(), eq(AppAuthority.WOMAN), any(), any(), applyBranchScope = eq(true)
            )
        } returns response

        mockMvc.perform(get("/api/v1/admin/female-employees"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].role").value("여사원"))
            .andExpect(jsonPath("$.data.content[0].jikjong").value("OSPM"))
            .andExpect(jsonPath("$.data.content[0].workEmail").value("kim@otoki.com"))
            .andExpect(jsonPath("$.data.content[0].phone").value("01012345678"))
            .andExpect(jsonPath("$.data.content[0].age").value("45살"))
            .andExpect(jsonPath("$.data.content[0].yearsOfService").value("5년"))

        // 여사원 현황은 본인 지점 스코프 적용 (applyBranchScope=true) 로 호출
        verify(exactly = 1) {
            adminEmployeeService.getEmployees(
                any(), any(), any(), any(), eq(AppAuthority.WOMAN), any(), any(), applyBranchScope = eq(true)
            )
        }
    }

    @Test
    @DisplayName("필터 파라미터 (status/costCenterCode/keyword/page/size) 전달")
    fun getFemaleEmployees_withFilters() {
        val response = EmployeeListResponse(
            content = emptyList(),
            page = 0,
            size = 10,
            totalElements = 0,
            totalPages = 0,
        )
        every {
            adminEmployeeService.getEmployees(
                any(), eq("재직"), eq("A001"), eq("김"), eq(AppAuthority.WOMAN), eq(0), eq(10),
                applyBranchScope = eq(true),
            )
        } returns response

        mockMvc.perform(
            get("/api/v1/admin/female-employees")
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
