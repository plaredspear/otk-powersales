package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterMeta
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterOption
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterType
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeListDefaults
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeListMetaResponse
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeManualRegisterRequest
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeManualRegisterService
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeService
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminFemaleEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminFemaleEmployeeController 테스트")
class AdminFemaleEmployeeControllerTest : AdminControllerTestSupport() {

    companion object {
        /** 여사원 현황 컨트롤러가 전달하는 role 목록 (여사원 + 조장) — 컨트롤러 상수와 동일. */
        private val FEMALE_EMPLOYEE_ROLES = listOf(AppAuthority.WOMAN, AppAuthority.LEADER)

        /**
         * 서비스가 내려주는 정적 메타 stub — 지점(costCenterCode) 은 포함하지 않는다.
         * 컨트롤러가 resolver 결과로 지점 필터를 뒤에 조립하는지 검증하기 위한 최소 형태.
         */
        private fun staticListMeta() = FemaleEmployeeListMetaResponse(
            filters = listOf(
                FemaleEmployeeFilterMeta(
                    key = "status",
                    type = FemaleEmployeeFilterType.SELECT,
                    options = listOf(FemaleEmployeeFilterOption("재직", "재직")),
                ),
            ),
            defaults = FemaleEmployeeListDefaults(pageSize = 20, sort = "name,ASC"),
        )
    }

    @MockkBean
    private lateinit var adminEmployeeService: AdminEmployeeService

    @MockkBean
    private lateinit var employeeWorkHistoryService: EmployeeWorkHistoryService

    @MockkBean
    private lateinit var adminEmployeeCredentialService: AdminEmployeeCredentialService

    @MockkBean
    private lateinit var adminEmployeeManualRegisterService: AdminEmployeeManualRegisterService

    @MockkBean
    private lateinit var womenScheduleBranchResolver: WomenScheduleBranchResolver

    @MockkBean
    private lateinit var branchCodeExpander: BranchCodeExpander

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubBranchScope() {
        // 목록/엑셀 지점 스코프 기본 stub — 전사 권한자로 두어 지점 필터 없이 조회되게 한다.
        // 지점 권한자 경로를 검증하는 테스트에서 override.
        every { womenScheduleBranchResolver.isAllBranchesUser(any()) } returns true
        // 이력 코드(BranchMapping) 확장은 기본적으로 pass-through — 확장 자체는 BranchCodeExpanderTest 책임.
        every { branchCodeExpander.expand(any()) } answers { firstArg<Collection<String>>().toSet() }
    }

    @BeforeEach
    fun stubArgumentResolver() {
        // 본 컨트롤러는 @CurrentDataScope 를 쓰지 않지만 (지점 스코프는 resolveBranchScope 가 산출),
        // JacksonConfig 가 resolver 빈을 요구하고 Spring 이 모든 핸들러 파라미터마다 supportsParameter 를
        // 호출하므로 stub 을 유지한다. resolveArgument 는 호출되지 않아 stub 하지 않는다.
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
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
                    professionalPromotionTeam = "라면세일조",
                    workType1 = "진열",
                    workType3 = "고정",
                    workAccountName = "테스트마트",
                    workAccountCode = "ACC001",
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
        )
        every {
            adminEmployeeService.getEmployees(
                any(), any(), any(), any(), any(), any(), any(),
                applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
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
            .andExpect(jsonPath("$.data.content[0].workType1").value("진열"))
            .andExpect(jsonPath("$.data.content[0].workType3").value("고정"))

        // 여사원 현황은 여사원+조장 role + 본인 지점 스코프 적용 (applyBranchScope=true) 로 호출
        verify(exactly = 1) {
            adminEmployeeService.getEmployees(
                any(), any(), any(), any(), any(), any(), any(),
                applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
            )
        }
    }

    @Test
    @DisplayName("지점 권한자 - /meta 셀렉터 옵션 지점을 조회하면 그 지점 스코프로 조회 (셀렉터↔조회 동일 출처)")
    fun getFemaleEmployees_selectorBranchIsQueryable() {
        // 조장 등 지점 권한자: 셀렉터 옵션 = 본인 costCenterCode 의 조직 트리 (A001 + 형제 A002).
        every { womenScheduleBranchResolver.isAllBranchesUser(any()) } returns false
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
            BranchResponse(branchCode = "A002", branchName = "서울2지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminEmployeeService.getEmployees(
                scope = capture(scopeSlot), status = any(), costCenterCode = any(), keyword = any(),
                role = any(), page = any(), size = any(), applyBranchScope = eq(true),
                roles = eq(FEMALE_EMPLOYEE_ROLES), workType1 = any(), workType3 = any(),
                professionalPromotionTeam = any(),
            )
        } returns EmployeeListResponse(emptyList(), 0, 20, 0, 0)

        // 셀렉터가 옵션으로 내려준 형제 지점(A002) 을 선택해 조회 — 본인 소속 지점이 아니어도 허용돼야 한다.
        mockMvc.perform(get("/api/v1/admin/female-employees").param("costCenterCode", "A002"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        // 스코프의 지점 집합이 셀렉터 옵션(조직 트리) 과 동일 출처 — A002 가 포함돼 NoAccess 가 아니다.
        assertThat(scopeSlot.captured.isAllBranches).isFalse()
        assertThat(scopeSlot.captured.branchCodes).containsExactlyInAnyOrder("A001", "A002")
        assertThat(scopeSlot.captured.effectiveBranchCodes("A002"))
            .isInstanceOf(EffectiveBranchResult.Filtered::class.java)
    }

    @Test
    @DisplayName("지점 권한자 - 권한 밖 지점 요청은 빈 목록 (IDOR 차단)")
    fun getFemaleEmployees_deniesBranchOutsideScope() {
        every { womenScheduleBranchResolver.isAllBranchesUser(any()) } returns false
        // 셀렉터 화이트리스트에 없는 지점(Z999) 요청 — 조직 트리는 A001 뿐이다.
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
        )

        mockMvc.perform(get("/api/v1/admin/female-employees").param("costCenterCode", "Z999"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isEmpty)
            .andExpect(jsonPath("$.data.totalElements").value(0))

        // 권한 밖 지점은 서비스 조회 자체를 하지 않는다.
        verify(exactly = 0) {
            adminEmployeeService.getEmployees(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/meta - 정적 필터 + 권한별 지점 옵션 조립 (다중 지점)")
    fun getListMeta_assemblesBranchOptions() {
        every { adminEmployeeService.getFemaleEmployeeListMetaStatic() } returns staticListMeta()
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
            BranchResponse(branchCode = "A002", branchName = "서울2지점"),
        )

        mockMvc.perform(get("/api/v1/admin/female-employees/meta"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.defaults.pageSize").value(20))
            .andExpect(jsonPath("$.data.defaults.sort").value("name,ASC"))
            .andExpect(jsonPath("$.data.filters[0].key").value("status"))
            // costCenterCode 필터가 정적 메타 뒤에 조립됨 (권한 의존 — 컨트롤러가 resolver 결과로 붙임)
            .andExpect(jsonPath("$.data.filters[1].key").value("costCenterCode"))
            .andExpect(jsonPath("$.data.filters[1].type").value("SELECT"))
            .andExpect(jsonPath("$.data.filters[1].options[0].value").value("A001"))
            .andExpect(jsonPath("$.data.filters[1].options[0].label").value("서울1지점"))
            .andExpect(jsonPath("$.data.filters[1].options[1].value").value("A002"))

        verify(exactly = 1) { womenScheduleBranchResolver.resolveBranches(any()) }
    }

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/meta - 조장 등 단일 지점: 지점 옵션 1건만 조립")
    fun getListMeta_singleBranch() {
        every { adminEmployeeService.getFemaleEmployeeListMetaStatic() } returns staticListMeta()
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
        )

        mockMvc.perform(get("/api/v1/admin/female-employees/meta"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.filters[1].key").value("costCenterCode"))
            .andExpect(jsonPath("$.data.filters[1].options.length()").value(1))
            .andExpect(jsonPath("$.data.filters[1].options[0].label").value("서울1지점"))
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
                any(), eq("재직"), eq("A001"), eq("김"), any(), eq(0), eq(10),
                applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
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

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/export - Excel byte 응답 + Content-Disposition")
    fun exportFemaleEmployees_success() {
        val result = ExcelResult(bytes = ByteArray(800), filename = "여사원현황_20260618.xlsx")
        every {
            adminEmployeeService.exportEmployees(
                any(), any(), any(), any(), any(),
                applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
            )
        } returns result

        mockMvc.perform(get("/api/v1/admin/female-employees/export"))
            .andExpect(status().isOk)
            .andExpect(
                header().string(
                    "Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
            .andExpect(header().exists("Content-Disposition"))
    }

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/export - 검색 필터가 여사원+조장 role + 본인 지점 스코프로 전달")
    fun exportFemaleEmployees_filterParams() {
        val result = ExcelResult(bytes = ByteArray(10), filename = "여사원현황.xlsx")
        every {
            adminEmployeeService.exportEmployees(
                any(), eq("재직"), eq("A001"), eq("김"), any(),
                applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
            )
        } returns result

        mockMvc.perform(
            get("/api/v1/admin/female-employees/export")
                .param("status", "재직")
                .param("costCenterCode", "A001")
                .param("keyword", "김"),
        )
            .andExpect(status().isOk)

        verify {
            adminEmployeeService.exportEmployees(
                any(), eq("재직"), eq("A001"), eq("김"), any(),
                applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
            )
        }
    }

    @Test
    @DisplayName("export - 권한 밖 지점 요청은 NO_ACCESS 스코프 전달 → 헤더만 있는 빈 엑셀 (IDOR 차단)")
    fun exportFemaleEmployees_deniesBranchOutsideScope() {
        every { womenScheduleBranchResolver.isAllBranchesUser(any()) } returns false
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminEmployeeService.exportEmployees(
                scope = capture(scopeSlot), status = any(), costCenterCode = any(), keyword = any(),
                role = any(), applyBranchScope = eq(true), roles = eq(FEMALE_EMPLOYEE_ROLES),
                workType1 = any(), workType3 = any(), professionalPromotionTeam = any(),
            )
        } returns ExcelResult(bytes = ByteArray(10), filename = "여사원현황.xlsx")

        mockMvc.perform(get("/api/v1/admin/female-employees/export").param("costCenterCode", "Z999"))
            .andExpect(status().isOk)

        // branchCodes 가 비어 있어 서비스의 effectiveBranchCodes 가 NoAccess → 빈 목록으로 export.
        assertThat(scopeSlot.captured.isAllBranches).isFalse()
        assertThat(scopeSlot.captured.branchCodes).isEmpty()
        assertThat(scopeSlot.captured.effectiveBranchCodes("Z999"))
            .isEqualTo(EffectiveBranchResult.NoAccess)
    }

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/{id} - 여사원 단건 상세 조회 (employee READ 와 동일 응답)")
    fun getFemaleEmployee_success() {
        every { adminEmployeeService.getEmployee(7L) } returns mockk(relaxed = true) {
            every { id } returns 7L
        }

        mockMvc.perform(get("/api/v1/admin/female-employees/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(exactly = 1) { adminEmployeeService.getEmployee(7L) }
    }

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/{id}/work-history - 근무이력 조회 (limit 기본 10)")
    fun getFemaleEmployeeWorkHistory_success() {
        every { employeeWorkHistoryService.getRecentHistory(7L, 10) } returns
            EmployeeWorkHistoryResponse(items = emptyList())

        mockMvc.perform(get("/api/v1/admin/female-employees/7/work-history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray)

        verify(exactly = 1) { employeeWorkHistoryService.getRecentHistory(7L, 10) }
    }

    @Test
    @DisplayName("GET /api/v1/admin/female-employees/{id}/work-history/monthly - 월별 근무내역 조회")
    fun getFemaleEmployeeMonthlyWorkHistory_success() {
        every { employeeWorkHistoryService.getMonthlyHistory(7L, java.time.YearMonth.of(2026, 6)) } returns
            EmployeeWorkHistoryResponse(items = emptyList())

        mockMvc.perform(
            get("/api/v1/admin/female-employees/7/work-history/monthly").param("yearMonth", "2026-06"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(exactly = 1) { employeeWorkHistoryService.getMonthlyHistory(7L, java.time.YearMonth.of(2026, 6)) }
    }

    @Test
    @DisplayName("POST /api/v1/admin/female-employees/{id}/reset-device - 단말 초기화 위임 (female_employee:EDIT 가드)")
    fun resetFemaleEmployeeDevice_success() {
        every { adminEmployeeCredentialService.resetDevice(7L) } returns mockk(relaxed = true)

        mockMvc.perform(post("/api/v1/admin/female-employees/7/reset-device"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("단말이 초기화되었습니다"))

        verify(exactly = 1) { adminEmployeeCredentialService.resetDevice(7L) }
    }

    @Test
    @DisplayName("POST /api/v1/admin/female-employees/{id}/reset-password - 비밀번호 초기화 위임 (female_employee:EDIT 가드)")
    fun resetFemaleEmployeePassword_success() {
        every { adminEmployeeCredentialService.resetPassword(7L) } returns mockk(relaxed = true)

        mockMvc.perform(post("/api/v1/admin/female-employees/7/reset-password"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("비밀번호가 초기화되었습니다"))

        verify(exactly = 1) { adminEmployeeCredentialService.resetPassword(7L) }
    }

    @Test
    @DisplayName("POST /api/v1/admin/female-employees/manual - 수동 등록 위임 + 201 (female_employee:CREATE 가드)")
    fun manualRegisterFemaleEmployee_success() {
        val requestSlot = slot<AdminEmployeeManualRegisterRequest>()
        every { adminEmployeeManualRegisterService.register(capture(requestSlot)) } returns
            mockk(relaxed = true)

        mockMvc.perform(
            post("/api/v1/admin/female-employees/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode":"100123","name":"김여사","role":"여사원"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("사원이 등록되었습니다"))

        // 공용 endpoint 와 동일 service 재사용 — 등록 정책(origin=MANUAL 등) 분기 없음.
        verify(exactly = 1) { adminEmployeeManualRegisterService.register(any()) }
        assertThat(requestSlot.captured.employeeCode).isEqualTo("100123")
        assertThat(requestSlot.captured.name).isEqualTo("김여사")
    }

    @Test
    @DisplayName("POST /api/v1/admin/female-employees/manual - 필수값 누락은 400 (service 미호출)")
    fun manualRegisterFemaleEmployee_validationError() {
        mockMvc.perform(
            post("/api/v1/admin/female-employees/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"김여사"}"""),
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { adminEmployeeManualRegisterService.register(any()) }
    }
}
