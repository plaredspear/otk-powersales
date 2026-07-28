package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeService
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.DismissalPolicy
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterType
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.schedule.repository.LatestAttendanceInfo
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.verify
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.io.ByteArrayInputStream

@DisplayName("AdminEmployeeService 테스트")
class AdminEmployeeServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()

    /**
     * 지점 코드 확장 — 기본은 항등(입력 그대로) stub.
     * 조직 개편 이력 코드 확장 자체를 검증하는 테스트만 계보를 별도 stub 한다.
     */
    private val branchCodeExpander: BranchCodeExpander = mockk {
        every { expand(any()) } answers { firstArg<Collection<String>>().toSet() }
    }

    private val adminEmployeeService = AdminEmployeeService(
        employeeRepository,
        EmployeeListExcelExporter(),
        teamMemberScheduleRepository,
        organizationRepository,
        branchCodeExpander,
    )

    init {
        // 근무형태/근무거래처 조회 — 본 테스트들은 해당 컬럼을 검증하지 않으므로 기본 빈 결과로 stub.
        every {
            teamMemberScheduleRepository.findLatestAttendanceInfoByEmployeeIds(any())
        } returns emptyMap<Long, LatestAttendanceInfo>()
        // 근무형태 필터 매칭 employee_id 산출 — 기본 빈 목록. 근무형태 필터를 검증하는 테스트에서 override.
        every {
            teamMemberScheduleRepository.findEmployeeIdsByLatestWorkType(any(), any())
        } returns emptyList()
    }

    @Nested
    @DisplayName("getEmployees - 사원 목록 조회")
    inner class GetEmployeesTests {

        @Test
        @DisplayName("전체 권한 - 필터 없이 조회 -> 전체 사원 반환")
        fun allBranches_noFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", name = "홍길동"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, null, null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].employeeCode).isEqualTo("10000001")
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("직종명은 직무코드(jobCode) 기준 파생 - 레이디직→OSC직, 판촉/OSC 외는 원본 jikjong")
        fun jikjong_derivedFromJobCode() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // 판정 축은 jobCode 단일 — jikwee 는 어떤 값이든 결과에 영향을 주지 않는다
            // (과거 jikwee 기준 파생에서 전환. 대시보드 인원현황 도넛과 동일 축).
            val employees = listOf(
                createEmployee(employeeCode = "1", name = "A").apply { jobCode = "판촉직"; jikwee = "OSC"; jikjong = "원본M" },
                createEmployee(employeeCode = "2", name = "B").apply { jobCode = "OSC직"; jikwee = "OSPM"; jikjong = "원본C" },
                // 구 OSC (SAP A053, 2024-01-02 개명 이전 적재분) → OSC직 으로 흡수
                createEmployee(employeeCode = "3", name = "C").apply { jobCode = "레이디직"; jikwee = null; jikjong = "원본L" },
                createEmployee(employeeCode = "4", name = "D").apply { jobCode = "영업직"; jikwee = "OSPE"; jikjong = "관리직" },
                createEmployee(employeeCode = "5", name = "E").apply { jobCode = null; jikwee = "OSPJ"; jikjong = null },
            )
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 5L)
            every { employeeRepository.findEmployees(null, null, null, null, null, any(), any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            val jikjongByCode = result.content.associate { it.employeeCode to it.jikjong }
            assertThat(jikjongByCode["1"]).isEqualTo("판촉직")
            assertThat(jikjongByCode["2"]).isEqualTo("OSC직")
            assertThat(jikjongByCode["3"]).isEqualTo("OSC직")  // 레이디직 → OSC직 흡수
            assertThat(jikjongByCode["4"]).isEqualTo("관리직")  // 여사원 직무 외 → 원본 jikjong
            assertThat(jikjongByCode["5"]).isNull()            // jobCode·원본 모두 없음
        }

        @Test
        @DisplayName("직무 필터 - 'OSC직' 선택 시 구 명칭 '레이디직' 을 포함한 집합으로 조회")
        fun jobCodeFilter_oscIncludesLady() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)
            every {
                employeeRepository.findEmployees(
                    null, null, null, null, null, any(), any(), any(), any(), any(),
                    jobCodes = setOf("OSC직", "레이디직"),
                )
            } returns page

            adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20, jobCode = "OSC직")

            verify {
                employeeRepository.findEmployees(
                    null, null, null, null, null, any(), any(), any(), any(), any(),
                    jobCodes = setOf("OSC직", "레이디직"),
                )
            }
        }

        @Test
        @DisplayName("직무 필터 - '판촉직' 은 단일 값 집합으로 조회")
        fun jobCodeFilter_promotionSingleValue() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)
            every {
                employeeRepository.findEmployees(
                    null, null, null, null, null, any(), any(), any(), any(), any(),
                    jobCodes = setOf("판촉직"),
                )
            } returns page

            adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20, jobCode = "판촉직")

            verify {
                employeeRepository.findEmployees(
                    null, null, null, null, null, any(), any(), any(), any(), any(),
                    jobCodes = setOf("판촉직"),
                )
            }
        }

        @Test
        @DisplayName("직무 필터 - 유효하지 않은 값은 IllegalArgumentException")
        fun jobCodeFilter_invalidValue() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            assertThatThrownBy {
                adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20, jobCode = "레이디직")
            }.isInstanceOf(IllegalArgumentException::class.java)

            assertThatThrownBy {
                adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20, jobCode = "없는직무")
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("전체 권한 + 지점 필터 -> 지정 지점만 조회")
        fun allBranches_withCostCenterCode() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "A001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, listOf("A001"), null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, "A001", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].costCenterCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("스코프 미적용(기본) - 지점 권한 사용자도 필터 없이 조회 시 전사 반환")
        fun noScope_branchOnly_noFilter() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val employees = listOf(
                createEmployee(employeeCode = "10000001", costCenterCode = "A001"),
                createEmployee(employeeCode = "10000002", costCenterCode = "B002"),
            )
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 2L)
            // applyBranchScope=false (행사 그리드/사원 목록 등) — 지점 보안축 미적용. costCenterCode 미지정 → 전사.
            every { employeeRepository.findEmployees(null, null, null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(2)
        }

        @Test
        @DisplayName("스코프 미적용(기본) - costCenterCode 는 순수 표시 필터 (권한 외 지점도 차단 안 함)")
        fun noScope_branchOnly_displayFilterOnly() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000002", costCenterCode = "B002"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // scope.branchCodes=["A001"] 이지만 applyBranchScope=false 라 표시 필터(B002)를 그대로 전달
            every { employeeRepository.findEmployees(null, listOf("B002"), null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, "B002", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].costCenterCode).isEqualTo("B002")
        }

        @Test
        @DisplayName("스코프 미적용(기본) - branchCodes 비어있어도 전사 반환")
        fun noScope_branchOnly_emptyBranchCodes() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // applyBranchScope=false — 보안축 없음. costCenterCode 미지정 → 전사(branchFilter=null)
            every { employeeRepository.findEmployees(null, null, null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("스코프 적용 - 전사 권한자는 필터 없이 전사 반환")
        fun scope_allBranches_noFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // applyBranchScope=true + isAllBranches → EffectiveBranchResult.All → branchFilter=null
            every { employeeRepository.findEmployees(null, null, null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(
                scope, null, null, null, null, 0, 20, applyBranchScope = true
            )

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("스코프 적용 - 지점 권한자는 본인 지점으로 제한 (여사원 현황/스케줄 lookup)")
        fun scope_branchOnly_restrictsToOwnBranch() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "A001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // applyBranchScope=true + 미지정 → EffectiveBranchResult.Filtered(["A001","A002"])
            every {
                employeeRepository.findEmployees(null, listOf("A001", "A002"), null, null, null, any(), any(), any(), any(), any())
            } returns page

            val result = adminEmployeeService.getEmployees(
                scope, null, null, null, null, 0, 20, applyBranchScope = true
            )

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("스코프 적용 - 지점 선택 시에도 조직 개편 이력 코드가 필터에 포함된다 (대시보드와 동일 축)")
        fun scope_selectedBranch_expandsHistoricalCodes() {
            val scope = DataScope(branchCodes = listOf("5824"), isAllBranches = false)
            // 강남2지점(5824) 의 계보 — 조직 개편 이전 코드 5668 (branch_mapping 실데이터 정합)
            every { branchCodeExpander.expand(listOf("5824")) } returns setOf("5824", "5668")

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "5824"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every {
                employeeRepository.findEmployees(
                    null, match { it?.toSet() == setOf("5824", "5668") }, null, null, null,
                    any(), any(), any(), any(), any(),
                )
            } returns page

            val result = adminEmployeeService.getEmployees(
                scope, null, "5824", null, null, 0, 20, applyBranchScope = true,
            )

            assertThat(result.content).hasSize(1)
            verify {
                employeeRepository.findEmployees(
                    null, match { it?.toSet() == setOf("5824", "5668") }, null, null, null,
                    any(), any(), any(), any(), any(),
                )
            }
        }

        @Test
        @DisplayName("스코프 미적용(전사 검색) 은 표시 필터를 확장하지 않는다")
        fun scope_notApplied_doesNotExpandDisplayFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)
            every { employeeRepository.findEmployees(null, listOf("5824"), null, null, null, any(), any(), any(), any(), any()) } returns page

            adminEmployeeService.getEmployees(
                scope, null, "5824", null, null, 0, 20, applyBranchScope = false,
            )

            verify(exactly = 0) { branchCodeExpander.expand(any()) }
        }

        @Test
        @DisplayName("스코프 적용 - 권한 밖 지점 요청 시 빈 결과 (NoAccess)")
        fun scope_branchOnly_outOfScopeBranch_returnsEmpty() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            // 권한(A001) 밖 지점(B002) 요청 → EffectiveBranchResult.NoAccess → repository 미호출
            val result = adminEmployeeService.getEmployees(
                scope, null, "B002", null, null, 0, 20, applyBranchScope = true
            )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.totalPages).isEqualTo(0)
        }

        @Test
        @DisplayName("스코프 적용 - 권한 내 단일 지점 선택 시 그 지점만 조회")
        fun scope_branchOnly_inScopeBranch_filtersToSelected() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "A001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // applyBranchScope=true + 권한 내 A001 선택 → Filtered(["A001"])
            every { employeeRepository.findEmployees(null, listOf("A001"), null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(
                scope, null, "A001", null, null, 0, 20, applyBranchScope = true
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].costCenterCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("상태 필터 적용 -> 해당 상태 사원만 반환")
        fun statusFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", status = "재직"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees("재직", null, null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, "재직", null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("키워드 필터 적용 -> 사번/이름 부분 일치")
        fun keywordFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", name = "홍길동"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, null, "홍", null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, "홍", null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("근무형태 문자열 필터를 enum 으로 파싱해 매칭 employee_id 산출 후 findEmployees 에 IN 으로 전달")
        fun workTypeAndPromotionFilterParsedToRepository() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)
            // 근무형태(행사/순회) 문자열(displayName)로 native 쿼리 호출 → 매칭 employee_id [42] 산출 가정.
            every {
                teamMemberScheduleRepository.findEmployeeIdsByLatestWorkType("행사", "순회")
            } returns listOf(42L)
            every {
                employeeRepository.findEmployees(
                    null, null, null, null, null,
                    setOf(42L), ProfessionalPromotionTeamType.RAMEN_SALE, false,
                    false,
                    any(),
                )
            } returns page

            adminEmployeeService.getEmployees(
                scope, null, null, null, null, 0, 20,
                workType1 = "행사", workType3 = "순회", professionalPromotionTeam = "라면세일조",
            )

            // 근무형태 필터는 team_member_schedule 매칭 집합으로 먼저 변환되고(displayName 문자열 전달),
            verify {
                teamMemberScheduleRepository.findEmployeeIdsByLatestWorkType("행사", "순회")
            }
            // 그 집합이 findEmployees 의 workTypeMatchedEmployeeIds 로 전달된다 (상관 서브쿼리 제거).
            verify {
                employeeRepository.findEmployees(
                    null, null, null, null, null,
                    setOf(42L), ProfessionalPromotionTeamType.RAMEN_SALE, false,
                    false,
                    any(),
                )
            }
        }

        @Test
        @DisplayName("전문행사조 '일반' 필터 -> promotionTeamGeneral=true 로 전달 (IS NULL)")
        fun promotionTeamGeneralParsed() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)
            every {
                employeeRepository.findEmployees(null, null, null, null, null, null, null, true, false, any())
            } returns page

            adminEmployeeService.getEmployees(
                scope, null, null, null, null, 0, 20, professionalPromotionTeam = "일반",
            )

            verify {
                employeeRepository.findEmployees(null, null, null, null, null, null, null, true, false, any())
            }
        }

        @Test
        @DisplayName("전문행사조 '행사조 전체' 필터 -> promotionTeamAssignedOnly=true 로 전달 (일반 제외)")
        fun promotionTeamAssignedOnlyParsed() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)
            every {
                employeeRepository.findEmployees(null, null, null, null, null, null, null, false, true, any())
            } returns page

            adminEmployeeService.getEmployees(
                scope, null, null, null, null, 0, 20, professionalPromotionTeam = "행사조 전체",
            )

            verify {
                employeeRepository.findEmployees(null, null, null, null, null, null, null, false, true, any())
            }
        }

        @Test
        @DisplayName("유효하지 않은 근무형태 값 -> IllegalArgumentException")
        fun invalidWorkTypeThrows() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            assertThatThrownBy {
                adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20, workType1 = "존재안함")
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("지점 권한 + 허용 지점 필터 -> 해당 지점 사원 반환")
        fun branchOnly_allowedBranch() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "A001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, listOf("A001"), null, null, null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, "A001", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("여사원 현황 - roles(여사원+조장) 다중 필터를 repository 에 그대로 전달")
        fun roles_multiRoleFilter_passedThrough() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val employees = listOf(
                createEmployee(employeeCode = "10000001", role = "여사원"),
                createEmployee(employeeCode = "10000002", role = "조장"),
            )
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 2L)
            every {
                employeeRepository.findEmployees(null, listOf("A001"), null, null, listOf("여사원", "조장"), any(), any(), any(), any(), any())
            } returns page

            val result = adminEmployeeService.getEmployees(
                scope, null, null, null, null, 0, 20,
                applyBranchScope = true, roles = listOf("여사원", "조장"),
            )

            assertThat(result.content).hasSize(2)
            verify {
                employeeRepository.findEmployees(null, listOf("A001"), null, null, listOf("여사원", "조장"), any(), any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("getEmployee - 단건 상세 조회 (UC-04)")
    inner class GetEmployeeTests {

        @Test
        @DisplayName("존재하는 사원 -> 6개 그룹 필드 응답")
        fun success() {
            val employee = createEmployee(id = 42L, employeeCode = "10000099", name = "김여사")
                .apply {
                    jobCode = "판촉직"
                    jikwee = "사원"
                    homePhone = "010-1234-5678"
                }
            every { employeeRepository.findWithEmployeeInfoById(42L) } returns employee

            val result = adminEmployeeService.getEmployee(42L)

            assertThat(result.id).isEqualTo(42L)
            assertThat(result.employeeCode).isEqualTo("10000099")
            assertThat(result.name).isEqualTo("김여사")
            assertThat(result.jobCode).isEqualTo("판촉직")
            assertThat(result.homePhone).isEqualTo("010-1234-5678")
        }

        @Test
        @DisplayName("발령명 '면직' -> treatDismissalAsResigned=true 면 '퇴직(면직)', 기본값이면 원본 상태")
        fun dismissalDisplayStatus() {
            val employee = createEmployee(id = 43L, employeeCode = "10000100", name = "면직자")
                .apply {
                    status = "재직"
                    ordDetailNode = DismissalPolicy.ORD_DETAIL_NODE
                }
            every { employeeRepository.findWithEmployeeInfoById(43L) } returns employee

            // 여사원 현황 상세 — 목록과 동일 표기
            assertThat(adminEmployeeService.getEmployee(43L, treatDismissalAsResigned = true).status)
                .isEqualTo(DismissalPolicy.DISPLAY_STATUS)
            // 전체 사원 관리 상세 — SAP 원본 상태 그대로
            assertThat(adminEmployeeService.getEmployee(43L).status).isEqualTo("재직")
        }

        @Test
        @DisplayName("존재하지 않는 사원 -> EmployeeNotFoundException")
        fun notFound() {
            every { employeeRepository.findWithEmployeeInfoById(999L) } returns null

            assertThatThrownBy { adminEmployeeService.getEmployee(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
                .hasMessageContaining("999")
        }
    }

    @Nested
    @DisplayName("exportEmployees - 여사원 현황 엑셀 다운로드")
    inner class ExportEmployeesTests {

        @Test
        @DisplayName("성공 - 검색결과 전량을 헤더 19컬럼 + 데이터 행으로 출력 + 파일명 패턴")
        fun export_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val employees = listOf(
                createEmployee(employeeCode = "10000001", name = "홍길동"),
                createEmployee(employeeCode = "10000002", name = "김영희"),
            )
            val page = PageImpl(employees, PageRequest.of(0, 50_000, Sort.by("name").ascending()), 2L)
            every { employeeRepository.findEmployees(null, null, null, "여사원", null, any(), any(), any(), any(), any()) } returns page

            val result = adminEmployeeService.exportEmployees(
                scope, null, null, null, "여사원", applyBranchScope = true
            )

            assertThat(result.filename).startsWith("여사원현황_").endsWith(".xlsx")
            val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.sheetName).isEqualTo("여사원현황")
            assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("사번")
            assertThat(sheet.getRow(0).getCell(4).stringCellValue).isEqualTo("전문행사조")
            assertThat(sheet.getRow(0).getCell(5).stringCellValue).isEqualTo("근무형태1")
            assertThat(sheet.getRow(0).getCell(6).stringCellValue).isEqualTo("근무형태3")
            assertThat(sheet.getRow(0).getCell(7).stringCellValue).isEqualTo("근무거래처")
            assertThat(sheet.getRow(0).getCell(8).stringCellValue).isEqualTo("거래처코드")
            assertThat(sheet.getRow(0).getCell(18).stringCellValue).isEqualTo("앱활성")
            assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("10000001")
            assertThat(sheet.getRow(1).getCell(1).stringCellValue).isEqualTo("홍길동")
            assertThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("10000002")
            workbook.close()
        }

        @Test
        @DisplayName("성공 - 필터 파라미터가 repository 에 전달 + 50,000 페이지로 전량 조회")
        fun export_filterAndPageSize() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            val page = PageImpl(emptyList<Employee>(), PageRequest.of(0, 50_000, Sort.by("name").ascending()), 0L)
            every { employeeRepository.findEmployees("재직", null, "홍", "여사원", null, any(), any(), any(), any(), any()) } returns page

            adminEmployeeService.exportEmployees(scope, "재직", null, "홍", "여사원", applyBranchScope = true)

            verify {
                employeeRepository.findEmployees(
                    "재직", null, "홍", "여사원", null,
                    any(), any(), any(), any(),
                    match { it.pageSize == 50_000 }
                )
            }
        }

        @Test
        @DisplayName("지점 스코프 - 권한 밖 지점 요청(NoAccess) -> 쿼리 없이 헤더만 빈 엑셀")
        fun export_noAccess() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val result = adminEmployeeService.exportEmployees(
                scope, null, "B002", null, "여사원", applyBranchScope = true
            )

            val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
            assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(0) // 헤더 행만
            workbook.close()
            verify(exactly = 0) {
                employeeRepository.findEmployees(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("getFemaleEmployeeListMetaStatic - 여사원 현황 목록 조회 조건 로드(정적)")
    inner class GetFemaleEmployeeListMetaStaticTests {

        @Test
        @DisplayName("재직상태 옵션 = 재직/휴직/퇴직 서버 상수 3종")
        fun statusOptions() {
            val result = adminEmployeeService.getFemaleEmployeeListMetaStatic()

            val status = result.filters.first { it.key == "status" }
            assertThat(status.type).isEqualTo(FemaleEmployeeFilterType.SELECT)
            assertThat(status.options).extracting("value")
                .containsExactly("재직", "휴직", "퇴직")
        }

        @Test
        @DisplayName("근무형태1/3 옵션 = WorkingCategory1/3 displayName (선언 순서 = SF picklist 순서)")
        fun workTypeOptions() {
            val result = adminEmployeeService.getFemaleEmployeeListMetaStatic()

            assertThat(result.filters.first { it.key == "workType1" }.options).extracting("value")
                .containsExactly("진열", "행사")
            assertThat(result.filters.first { it.key == "workType3" }.options).extracting("value")
                .containsExactly("고정", "격고", "순회")
        }

        @Test
        @DisplayName("전문행사조 옵션 = '행사조 전체'(일반 제외) 선두 + '일반'(미배정) + 정식 5개 조 (SF picklist 정의 순서)")
        fun promotionTeamOptions() {
            val result = adminEmployeeService.getFemaleEmployeeListMetaStatic()

            val team = result.filters.first { it.key == "professionalPromotionTeam" }
            assertThat(team.options).extracting("value").containsExactly(
                "행사조 전체",
                "일반",
                "라면세일조",
                "프레시세일조_냉동",
                "프레시세일조_냉장",
                "프레시세일조_만두",
                "카레세일조",
            )
        }

        @Test
        @DisplayName("텍스트 필터는 options 없음, 정적 메타에 costCenterCode 미포함(권한 의존은 컨트롤러 조립)")
        fun textFilterAndNoBranch() {
            val result = adminEmployeeService.getFemaleEmployeeListMetaStatic()

            assertThat(result.filters.first { it.key == "keyword" }.type).isEqualTo(FemaleEmployeeFilterType.TEXT)
            assertThat(result.filters.first { it.key == "keyword" }.options).isNull()
            assertThat(result.filters.map { it.key }).doesNotContain("costCenterCode")
        }

        @Test
        @DisplayName("기본값 — pageSize 20 / sort name,ASC (getEmployees 실제 정렬과 일치)")
        fun defaults() {
            val result = adminEmployeeService.getFemaleEmployeeListMetaStatic()

            assertThat(result.defaults.pageSize).isEqualTo(20)
            assertThat(result.defaults.sort).isEqualTo("name,ASC")
        }
    }

    @Nested
    @DisplayName("getFemaleEmployeeFormMeta - 여사원 상세 폼(수정 모달) 옵션 로드")
    inner class GetFemaleEmployeeFormMetaTests {

        @Test
        @DisplayName("재직상태 옵션 = 재직/휴직/퇴직 (목록 필터와 동일 출처)")
        fun statusOptions() {
            val result = adminEmployeeService.getFemaleEmployeeFormMeta()

            assertThat(result.statuses).extracting("value").containsExactly("재직", "휴직", "퇴직")
        }

        @Test
        @DisplayName("권한 옵션 = SF AppAuthority picklist 4종, AccountViewAll 만 label 에 한글 병기")
        fun roleOptions() {
            val result = adminEmployeeService.getFemaleEmployeeFormMeta()

            assertThat(result.roles).extracting("value")
                .containsExactly("여사원", "조장", "지점장", "AccountViewAll")
            // raw value = label 인 3종과 달리 AccountViewAll 은 의미가 안 드러나 한글 병기
            assertThat(result.roles.first { it.value == "AccountViewAll" }.label)
                .isEqualTo("영업부장 (AccountViewAll)")
            assertThat(result.roles.first { it.value == "여사원" }.label).isEqualTo("여사원")
        }

        @Test
        @DisplayName("전문행사조 옵션 = '일반'(미배정) 선두 + 정식 5개 조 (SF picklist 정의 순서)")
        fun promotionTeamOptions() {
            val result = adminEmployeeService.getFemaleEmployeeFormMeta()

            assertThat(result.professionalPromotionTeams).extracting("value").containsExactly(
                "일반",
                "라면세일조",
                "프레시세일조_냉동",
                "프레시세일조_냉장",
                "프레시세일조_만두",
                "카레세일조",
            )
        }

        @Test
        @DisplayName("전문행사조 폼 옵션에는 검색 전용 '행사조 전체' 가 없다 (목록 필터와의 차이)")
        fun promotionTeamExcludesSearchOnlyOption() {
            val formMeta = adminEmployeeService.getFemaleEmployeeFormMeta()
            val listMeta = adminEmployeeService.getFemaleEmployeeListMetaStatic()

            // 폼은 저장 가능한 값만 내려야 하므로 검색 전용 선택지를 제외한다.
            assertThat(formMeta.professionalPromotionTeams).extracting("value")
                .doesNotContain(ProfessionalPromotionTeamType.ASSIGNED_ONLY_DISPLAY_NAME)
            // 목록 필터에는 그대로 남아 있어야 한다 (본 변경이 목록을 건드리지 않았음을 고정).
            assertThat(listMeta.filters.first { it.key == "professionalPromotionTeam" }.options)
                .extracting("value")
                .contains(ProfessionalPromotionTeamType.ASSIGNED_ONLY_DISPLAY_NAME)
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "10000001",
        name: String = "테스트",
        status: String? = "재직",
        costCenterCode: String? = "A001",
        role: String? = null
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name,
        status = status,
        costCenterCode = costCenterCode,
        role = role
    )
}
