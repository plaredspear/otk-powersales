package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.organization.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

@DisplayName("AdminTeamScheduleService 테스트")
class AdminTeamScheduleServiceTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)

    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk(relaxUnitFun = true)

    private val employeeRepository: EmployeeRepository = mockk(relaxUnitFun = true)

    private val accountRepository: AccountRepository = mockk(relaxUnitFun = true)

    private val organizationRepository: OrganizationRepository = mockk(relaxUnitFun = true)

    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService = mockk(relaxUnitFun = true)

    private val branchCodeExpander: BranchCodeExpander = mockk(relaxUnitFun = true)

    private lateinit var service: AdminTeamScheduleService

    @BeforeEach
    fun setUpService() {
        val teamScheduleValidator = TeamScheduleValidator(
            teamMemberScheduleRepository,
            displayWorkScheduleRepository
        )
        service = AdminTeamScheduleService(
            teamMemberScheduleRepository = teamMemberScheduleRepository,
            employeeRepository = employeeRepository,
            accountRepository = accountRepository,
            organizationRepository = organizationRepository,
            adminMonthlyIntegrationService = adminMonthlyIntegrationService,
            teamScheduleValidator = teamScheduleValidator,
            branchCodeExpander = branchCodeExpander
        )
        // BranchCodeExpander 는 SF Util.getIncludedBranchCode 정합 — 일반 cost center 는 입력=출력 (1:1).
        // 1:N 확장 케이스만 테스트하는 컨텍스트에서 개별 override.
        every { branchCodeExpander.expand(any()) } answers { firstArg<Collection<String>>().toSet() }
        // createSchedule 이 service 내부에서 leader entity 영속화를 위해 호출하는 findById(principal.employeeId).
        // 인증 컨텍스트 분기와 무관한 부수 호출이므로 기본 stub (개별 테스트에서 override 가능).
        every { employeeRepository.findById(any<Long>()) } returns Optional.of(createEmployee(id = 10L, sfid = "LEADER_SFID"))
    }

    // 인증된 현재 사용자 = WebUserPrincipal. fixture 는 분기에 영향 안 주는 케이스용 더미.
    // 권한/cost center 분기 케이스는 각 test 가 createEmployee(...) → principalOf(..) 로 변환해 직접 전달.
    private val currentEmployeeFixture: com.otoki.powersales.auth.web.WebUserPrincipal
        get() = principalOf(createEmployee(id = 10L, role = null))

    /** Employee fixture 를 service 시그니처가 요구하는 WebUserPrincipal 로 변환. */
    private fun principalOf(
        employee: Employee,
        profileName: String = "9. Staff",
        isSalesSupport: Boolean = false,
    ): com.otoki.powersales.auth.web.WebUserPrincipal =
        com.otoki.powersales.auth.web.WebUserPrincipal(
            userId = employee.id * 10,
            usernameValue = employee.employeeCode,
            employeeCode = employee.employeeCode,
            employeeId = employee.id,
            role = employee.role,
            costCenterCode = employee.costCenterCode,
            profileName = profileName,
            isSalesSupport = isSalesSupport,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )

    // --- Helper factories ---

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "USR_SFID_001",
        employeeCode: String = "20030001",
        name: String = "테스트사원",
        status: String? = "재직",
        role: String? = null,
        costCenterCode: String? = "1234",
        isDeleted: Boolean? = false
    ): Employee = Employee(
        id = id,
        sfid = sfid,
        employeeCode = employeeCode,
        name = name,
        status = status,
        role = role,
        costCenterCode = costCenterCode,
        isDeleted = isDeleted
    )

    private fun createAccount(
        id: Int = 1,
        sfid: String? = "ACC_SFID_001",
        name: String = "테스트거래처",
        externalKey: String? = "ACC001",
        accountGroup: String? = "1010",
        branchCode: String? = "1234",
        isDeleted: Boolean? = false
    ): Account = Account(
        id = id,
        sfid = sfid,
        name = name,
        externalKey = externalKey,
        accountGroup = accountGroup,
        branchCode = branchCode,
        isDeleted = isDeleted
    )

    /** UC-13 FK 매칭 검증용 dummy DisplayWorkSchedule — id 만 있으면 됨 (validator 는 null 체크만 수행). */
    private fun dummyDisplayMaster(): com.otoki.powersales.schedule.entity.DisplayWorkSchedule =
        com.otoki.powersales.schedule.entity.DisplayWorkSchedule(id = 9999L)

    private fun createSchedule(
        id: Long = 1L,
        employeeId: Long? = 1L,
        employeeCode: String? = null,
        employeeName: String? = null,
        workingDate: LocalDate? = LocalDate.of(2026, 4, 1),
        workingType: WorkingType? = WorkingType.WORK,
        workingCategory1: WorkingCategory1? = WorkingCategory1.DISPLAY,
        workingCategory2: WorkingCategory2? = null,
        workingCategory3: WorkingCategory3? = WorkingCategory3.FIXED,
        accountId: Int? = 1,
        accountName: String? = null,
        accountExternalKey: String? = null,
        teamLeaderId: Long? = 99L,
        commuteLogSfid: String? = null,
        displayWorkSchedule: com.otoki.powersales.schedule.entity.DisplayWorkSchedule? = null
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = employeeId?.let {
            Employee(
                id = it,
                employeeCode = employeeCode ?: "EMP$it",
                name = employeeName ?: "테스트$it"
            )
        },
        workingDate = workingDate,
        workingType = workingType,
        workingCategory1 = workingCategory1,
        workingCategory2 = workingCategory2,
        workingCategory3 = workingCategory3,
        account = accountId?.let {
            Account(
                id = it,
                name = accountName ?: "거래처$it",
                externalKey = accountExternalKey
            )
        },
        teamLeader = teamLeaderId?.let { Employee(id = it, employeeCode = "EMP$it", name = "팀장$it") },
        commuteLogSfid = commuteLogSfid,
        // Spec #789 정합 — 출근 등록 가드는 attendance_log id-FK 기준.
        attendanceLog = commuteLogSfid?.let { com.otoki.powersales.schedule.entity.AttendanceLog(id = 1L) },
        displayWorkSchedule = displayWorkSchedule
    )

    // ========== getMembers ==========

    @Nested
    @DisplayName("getMembers - 여사원 목록 조회 (SF 정합 — branchCode 무관)")
    inner class GetMembersTests {

        @Test
        @DisplayName("일반 조장 - 본인 costCenterCode 의 활성 WOMAN 반환")
        fun getMembers_leader_singleCostCenter() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", costCenterCode = "1234", role = AppAuthority.LEADER)
            val m1 = createEmployee(id = 2L, employeeCode = "20030002", name = "김영희", role = AppAuthority.WOMAN)
            val m2 = createEmployee(id = 3L, employeeCode = "20030003", name = "이수진", role = AppAuthority.WOMAN)

            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("1234")) } returns listOf(m1, m2)

            val result = service.getMembers(principalOf(leader))

            assertThat(result).hasSize(2)
            assertThat(result[0].employeeCode).isEqualTo("20030002")
            assertThat(result[1].employeeCode).isEqualTo("20030003")
        }

        @Test
        @DisplayName("SYSTEM_ADMIN - 본인 costCenterCode 단일 조회 (SF 여사원 모드는 지점 드롭다운 없음)")
        fun getMembers_systemAdmin_usesOwnCostCenter() {
            val admin = createEmployee(id = 10L, employeeCode = "99990001", costCenterCode = "9999", role = null)
            val m1 = createEmployee(id = 2L, employeeCode = "20030002", name = "김영희", role = AppAuthority.WOMAN)
            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("9999")) } returns listOf(m1)

            val result = service.getMembers(principalOf(admin))

            assertThat(result).hasSize(1)
            verify { employeeRepository.findActiveWomenByCostCenterCodes(listOf("9999")) }
        }

        @Test
        @DisplayName("특수 사번 (19951029) - cost center IN ('3233','3234','3235','3236','5691','5694')")
        fun getMembers_specialEmployeeCode() {
            val leader = createEmployee(id = 10L, employeeCode = "19951029", costCenterCode = "9999", role = AppAuthority.LEADER)
            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("3233", "3234", "3235", "3236", "5691", "5694")) } returns emptyList()

            service.getMembers(principalOf(leader))

            verify { employeeRepository.findActiveWomenByCostCenterCodes(listOf("3233", "3234", "3235", "3236", "5691", "5694")) }
        }

        @Test
        @DisplayName("영업지원1팀 (cost_center_code=4888) - 본인 costCenterCode 단일 조회 (SF 정합)")
        fun getMembers_salesSupport1Team() {
            val supporter = createEmployee(id = 10L, employeeCode = "20100001", costCenterCode = "4888", role = null)
            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("4888")) } returns emptyList()

            service.getMembers(principalOf(supporter))

            verify { employeeRepository.findActiveWomenByCostCenterCodes(listOf("4888")) }
        }

        @Test
        @DisplayName("costCenterCode null + 일반 Role - 빈 결과")
        fun getMembers_costCenterCodeNull_returnsEmpty() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", costCenterCode = null, role = AppAuthority.LEADER)

            val result = service.getMembers(principalOf(leader))

            assertThat(result).isEmpty()
            verify(exactly = 0) { employeeRepository.findActiveWomenByCostCenterCodes(any()) }
        }
    }

    // ========== getAccounts ==========

    @Nested
    @DisplayName("getAccounts - 거래처 목록 조회")
    inner class GetAccountsTests {

        @Test
        @DisplayName("정상 조회 - 사용자 branchCode로 거래처 목록 반환")
        fun getAccounts_success() {
            // Given
            val employee = createEmployee(id = 10L, costCenterCode = "1234")
            val account1 = createAccount(id = 1, sfid = "ACC_001", name = "이마트 강남점", branchCode = "1234")
            val account2 = createAccount(id = 2, sfid = "ACC_002", name = "홈플러스 역삼점", branchCode = "1234")

            every { accountRepository.findByBranchCodeInAndAccountGroupIn(setOf("1234"), listOf("1010", "1000")) } returns listOf(account1, account2)

            // When
            val result = service.getAccounts(principalOf(employee), null)

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].accountId).isEqualTo(1)
            assertThat(result[1].accountId).isEqualTo(2)
        }

        @Test
        @DisplayName("branch_code 파라미터 지정 - 지정 지점의 거래처 반환")
        fun getAccounts_withBranchCode() {
            // Given
            val account = createAccount(id = 1, sfid = "ACC_001", name = "롯데마트 잠실점", branchCode = "5678")

            every { accountRepository.findByBranchCodeInAndAccountGroupIn(setOf("5678"), listOf("1010", "1000")) } returns listOf(account)

            // When
            val result = service.getAccounts(currentEmployeeFixture, "5678")

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].accountId).isEqualTo(1)
            verify(exactly = 0) { employeeRepository.findWithEmployeeInfoById(any()) }
        }

        @Test
        @DisplayName("이름 가나다순 정렬 - 입력 순서와 무관하게 name asc 반환")
        fun getAccounts_sortedByName() {
            val employee = createEmployee(id = 10L, costCenterCode = "1234")
            val homeplus = createAccount(id = 1, sfid = "ACC_001", name = "홈플러스 역삼점", branchCode = "1234")
            val emart = createAccount(id = 2, sfid = "ACC_002", name = "이마트 강남점", branchCode = "1234")
            val gs = createAccount(id = 3, sfid = "ACC_003", name = "GS25 강남점", branchCode = "1234")

            every { accountRepository.findByBranchCodeInAndAccountGroupIn(setOf("1234"), listOf("1010", "1000")) } returns
                listOf(homeplus, emart, gs)

            val result = service.getAccounts(principalOf(employee), null)

            assertThat(result.map { it.name }).containsExactly("GS25 강남점", "이마트 강남점", "홈플러스 역삼점")
        }

        @Test
        @DisplayName("BranchMapping 1:N 확장 - cvs전략 '5694' → {5691,5692,5693,5694} 모두 조회")
        fun getAccounts_branchMappingExpansion() {
            // Given: SF customMetadata/BranchMapping.cvs.md-meta.xml 정합
            // BranchCode__c='5694' → IncludedBranchCode__c='5691,5692,5693,5694'
            val expandedCodes = setOf("5691", "5692", "5693", "5694")
            every { branchCodeExpander.expand(setOf("5694")) } returns expandedCodes

            val acc1 = createAccount(id = 1, sfid = "ACC_001", name = "CVS 1", branchCode = "5691")
            val acc2 = createAccount(id = 2, sfid = "ACC_002", name = "CVS 2", branchCode = "5692")
            val acc3 = createAccount(id = 3, sfid = "ACC_003", name = "CVS 3", branchCode = "5693")
            val acc4 = createAccount(id = 4, sfid = "ACC_004", name = "CVS 4", branchCode = "5694")

            every { accountRepository.findByBranchCodeInAndAccountGroupIn(expandedCodes, listOf("1010", "1000")) } returns
                listOf(acc1, acc2, acc3, acc4)

            // When
            val result = service.getAccounts(currentEmployeeFixture, "5694")

            // Then
            assertThat(result).hasSize(4)
            assertThat(result.map { it.accountId }).containsExactly(1, 2, 3, 4)
        }
    }

    // ========== getBranches ==========

    @Nested
    @DisplayName("getBranches - 지점 드롭다운 옵션 조회 (SF 정합)")
    inner class GetBranchesTests {

        @Test
        @DisplayName("SYSTEM_ADMIN - 전체 Organization 조회")
        fun getBranches_systemAdmin() {
            val admin = createEmployee(id = 10L, role = null, costCenterCode = "9999")
            val branches = listOf(
                com.otoki.powersales.common.dto.response.BranchResponse("5460", "강남유통지점"),
                com.otoki.powersales.common.dto.response.BranchResponse("5457", "강북유통지점")
            )
            every { organizationRepository.findAllTeamScheduleBranches() } returns branches

            val result = service.getBranches(principalOf(admin, profileName = "시스템 관리자"))

            assertThat(result).hasSize(2)
            assertThat(result[0].branchCode).isEqualTo("5460")
            verify(exactly = 0) { organizationRepository.findTeamScheduleBranches(any(), any()) }
        }

        @Test
        @DisplayName("ALL_BRANCHES Role (영업지원실) - 전사 분기 (CVS 미포함)")
        fun getBranches_allBranchesRole() {
            val supporter = createEmployee(id = 10L, role = null, costCenterCode = "3475")
            val branches = listOf(com.otoki.powersales.common.dto.response.BranchResponse("5460", "강남유통지점"))
            every { organizationRepository.findTeamScheduleBranches(null, true) } returns branches

            val result = service.getBranches(principalOf(supporter, isSalesSupport = true))

            assertThat(result).hasSize(1)
            assertThat(result[0].branchCode).isEqualTo("5460")
            verify(exactly = 0) { organizationRepository.findAllTeamScheduleBranches() }
            verify { organizationRepository.findTeamScheduleBranches(null, true) }
        }

        @Test
        @DisplayName("일반 영업담당 Role (조장) - 본인 costCenterCode 기준 분기")
        fun getBranches_scopedRole() {
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER, costCenterCode = "5457")
            val branches = listOf(com.otoki.powersales.common.dto.response.BranchResponse("5457", "강북유통지점"))
            every { organizationRepository.findTeamScheduleBranches("5457", false) } returns branches

            val result = service.getBranches(principalOf(leader))

            assertThat(result).hasSize(1)
            assertThat(result[0].branchCode).isEqualTo("5457")
            verify { organizationRepository.findTeamScheduleBranches("5457", false) }
        }
    }

    // ========== getForm ==========

    @Nested
    @DisplayName("getForm - 화면 초기 로드 통합 조회")
    inner class GetFormTests {

        @Test
        @DisplayName("단일지점 사용자 - branches 1건 + accounts 자동 채움")
        fun getForm_singleBranch_includesAccounts() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", costCenterCode = "5457", role = AppAuthority.LEADER)
            val branch = com.otoki.powersales.common.dto.response.BranchResponse("5457", "강북유통지점")
            val member = createEmployee(id = 2L, employeeCode = "20030002", name = "김영희", role = AppAuthority.WOMAN)
            val account = createAccount(id = 1, sfid = "ACC_001", name = "이마트 강북점", branchCode = "5457")

            every { organizationRepository.findTeamScheduleBranches("5457", false) } returns listOf(branch)
            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("5457")) } returns listOf(member)
            every { accountRepository.findByBranchCodeInAndAccountGroupIn(setOf("5457"), listOf("1010", "1000")) } returns listOf(account)
            // 단일지점 케이스는 accounts 가 채워지므로 dailySummary 계산을 위해 schedules 조회 발생
            every { teamMemberScheduleRepository.findMonthlyByAccountIds(eq(listOf(1)), any(), any(), isNull()) } returns emptyList()

            val result = service.getForm(principalOf(leader))

            assertThat(result.branches).hasSize(1)
            assertThat(result.branches[0].branchCode).isEqualTo("5457")
            assertThat(result.members).hasSize(1)
            assertThat(result.professionalPromotionTeams).containsExactly(
                "라면세일조", "프레시세일조_냉동", "프레시세일조_냉장", "프레시세일조_만두", "카레행사조"
            )
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountId).isEqualTo(1)
            assertThat(result.dailySummary).isEmpty()
        }

        @Test
        @DisplayName("다중지점 사용자 - branches 2건+, accounts 는 빈 리스트 (사용자가 지점 선택 후 별도 fetch)")
        fun getForm_multiBranch_accountsEmpty() {
            val supporter = createEmployee(id = 10L, employeeCode = "20100001", costCenterCode = "3475", role = null)
            val branches = listOf(
                com.otoki.powersales.common.dto.response.BranchResponse("5460", "강남유통지점"),
                com.otoki.powersales.common.dto.response.BranchResponse("5457", "강북유통지점")
            )

            every { organizationRepository.findTeamScheduleBranches(null, true) } returns branches
            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("3475")) } returns emptyList()

            val result = service.getForm(principalOf(supporter, isSalesSupport = true))

            assertThat(result.branches).hasSize(2)
            assertThat(result.accounts).isEmpty()
            verify(exactly = 0) { accountRepository.findByBranchCodeInAndAccountGroupIn(any(), any()) }
        }

        @Test
        @DisplayName("지점 0건 사용자 - accounts 빈 리스트, /accounts 호출 없음")
        fun getForm_noBranch_accountsEmpty() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", costCenterCode = "9999", role = AppAuthority.LEADER)

            every { organizationRepository.findTeamScheduleBranches("9999", false) } returns emptyList()
            every { employeeRepository.findActiveWomenByCostCenterCodes(listOf("9999")) } returns emptyList()

            val result = service.getForm(principalOf(leader))

            assertThat(result.branches).isEmpty()
            assertThat(result.accounts).isEmpty()
            verify(exactly = 0) { accountRepository.findByBranchCodeInAndAccountGroupIn(any(), any()) }
        }
    }

    // ========== getSchedulesWithSummary ==========

    @Nested
    @DisplayName("getSchedulesWithSummary - 기간 일정 + 일별 요약 통합 조회")
    inner class GetSchedulesWithSummaryTests {

        @Test
        @DisplayName("정상 조회 - employeeIds 지정 시 schedules와 dailySummary 모두 반환")
        fun getSchedulesWithSummary_byEmployeeIds() {
            val date = LocalDate.of(2026, 4, 1)
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 30)
            val displaySchedule = createSchedule(id = 1L, employeeId = 1L, employeeCode = "20030001", employeeName = "홍길동", workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY)
            val displayWithCommute = createSchedule(id = 2L, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, commuteLogSfid = "CL001")
            val promotionSchedule = createSchedule(id = 3L, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.EVENT)
            val promotionWithCommute = createSchedule(id = 4L, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.EVENT, commuteLogSfid = "CL002")
            val annualLeave = createSchedule(id = 5L, workingDate = date, workingType = WorkingType.ANNUAL_LEAVE, workingCategory1 = null)
            val compensatoryLeave = createSchedule(id = 6L, workingDate = date, workingType = WorkingType.ALT_HOLIDAY, workingCategory1 = null)

            val allSchedules = listOf(displaySchedule, displayWithCommute, promotionSchedule, promotionWithCommute, annualLeave, compensatoryLeave)

            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(eq(listOf(1L)), eq(from), eq(to), null) } returns allSchedules

            val result = service.getSchedulesWithSummary(from, to, listOf(1L), null)

            assertThat(result.schedules).hasSize(6)
            assertThat(result.schedules[0].employeeCode).isEqualTo("20030001")
            assertThat(result.schedules[0].employeeName).isEqualTo("홍길동")

            assertThat(result.dailySummary).hasSize(1)
            val summary = result.dailySummary[0]
            assertThat(summary.date).isEqualTo("2026-04-01")
            assertThat(summary.displayExpected).isEqualTo(2)
            assertThat(summary.displayActual).isEqualTo(1)
            assertThat(summary.promotionExpected).isEqualTo(2)
            assertThat(summary.promotionActual).isEqualTo(1)
            assertThat(summary.annualLeave).isEqualTo(1)
            assertThat(summary.compensatoryLeave).isEqualTo(1)
        }

        @Test
        @DisplayName("accountIds 지정 - 거래처 필터로 일정 반환")
        fun getSchedulesWithSummary_byAccountIds() {
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 30)
            val schedule = createSchedule(id = 1L, accountId = 10)

            every { teamMemberScheduleRepository.findMonthlyByAccountIds(eq(listOf(10)), eq(from), eq(to), null) } returns listOf(schedule)

            val result = service.getSchedulesWithSummary(from, to, null, listOf(10))

            assertThat(result.schedules).hasSize(1)
            assertThat(result.schedules[0].accountId).isEqualTo(10)
            assertThat(result.dailySummary).hasSize(1)
        }

        @Test
        @DisplayName("전문행사조 필터 - 비어있지 않은 리스트가 그대로 repository 에 전달")
        fun getSchedulesWithSummary_withPromotionTeams() {
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 30)
            val teams = listOf("라면세일조", "카레행사조")
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(eq(listOf(1L)), eq(from), eq(to), eq(teams)) } returns emptyList()

            service.getSchedulesWithSummary(from, to, listOf(1L), null, teams)

            verify { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, teams) }
        }

        @Test
        @DisplayName("전문행사조 필터 - blank/빈 값은 정리 후 전달 (전부 blank 면 null)")
        fun getSchedulesWithSummary_promotionTeamsBlankFiltered() {
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 30)
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(eq(listOf(1L)), eq(from), eq(to), null) } returns emptyList()

            service.getSchedulesWithSummary(from, to, listOf(1L), null, listOf("", "  "))

            verify { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) }
        }

        @Test
        @DisplayName("필터 없이 조회 - 빈 배열 반환")
        fun getSchedulesWithSummary_noFilter_returnsEmpty() {
            val result = service.getSchedulesWithSummary(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), null, null)

            assertThat(result.schedules).isEmpty()
            assertThat(result.dailySummary).isEmpty()
            verify { teamMemberScheduleRepository wasNot Called }
        }

        @Test
        @DisplayName("SF XOR - employeeIds 와 accountIds 동시 지정 시 employeeIds 만 사용")
        fun getSchedulesWithSummary_xor_employeePrefers() {
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 30)
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) } returns emptyList()

            service.getSchedulesWithSummary(from, to, listOf(1L), listOf(10))

            verify { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) }
            verify(exactly = 0) { teamMemberScheduleRepository.findMonthlyByAccountIds(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("기간 임의 지정 - from~to 가 그대로 repository 에 전달")
        fun getSchedulesWithSummary_customRange() {
            val from = LocalDate.of(2026, 4, 15)
            val to = LocalDate.of(2026, 7, 14)
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) } returns emptyList()

            service.getSchedulesWithSummary(from, to, listOf(1L), null)

            verify { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) }
        }

        @Test
        @DisplayName("기간 92일 초과 시 TeamScheduleRangeTooWideException")
        fun getSchedulesWithSummary_rangeTooWide() {
            val from = LocalDate.of(2026, 5, 1)
            val to = LocalDate.of(2026, 8, 1) // between = 92일 → 92 > 91 위반

            org.junit.jupiter.api.assertThrows<com.otoki.powersales.schedule.exception.TeamScheduleRangeTooWideException> {
                service.getSchedulesWithSummary(from, to, listOf(1L), null)
            }
            verify { teamMemberScheduleRepository wasNot Called }
        }

        @Test
        @DisplayName("기간 정확히 92일 (between=91) 은 통과")
        fun getSchedulesWithSummary_rangeExactly92Days() {
            val from = LocalDate.of(2026, 5, 1)
            val to = LocalDate.of(2026, 7, 31) // between = 91일 → 92일 분량
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) } returns emptyList()

            service.getSchedulesWithSummary(from, to, listOf(1L), null)

            verify { teamMemberScheduleRepository.findMonthlyByEmployeeIds(listOf(1L), from, to, null) }
        }

        @Test
        @DisplayName("from > to 일 때 TeamScheduleInvalidRangeException")
        fun getSchedulesWithSummary_invalidRange() {
            val from = LocalDate.of(2026, 7, 1)
            val to = LocalDate.of(2026, 5, 1)

            org.junit.jupiter.api.assertThrows<com.otoki.powersales.schedule.exception.TeamScheduleInvalidRangeException> {
                service.getSchedulesWithSummary(from, to, listOf(1L), null)
            }
            verify { teamMemberScheduleRepository wasNot Called }
        }
    }

    // ========== createSchedule ==========

    @Nested
    @DisplayName("createSchedule - 일정 등록")
    inner class CreateScheduleTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> 일정 ID 반환")
        fun createSchedule_success() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val account = createAccount(sfid = "ACC_SFID_001")
            val savedSchedule = createSchedule(id = 100L)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns emptyList()
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns savedSchedule

            // When
            val result = service.createSchedule(currentEmployeeFixture, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
            verify { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("휴직 사원 등록 - EMPLOYEE_ON_LEAVE")
        fun createSchedule_employeeOnLeave() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "휴직")
            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleEmployeeOnLeaveException::class.java)
        }

        @Test
        @DisplayName("퇴직 사원 등록 - EMPLOYEE_RESIGNED")
        fun createSchedule_employeeResigned() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "퇴직")
            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleEmployeeResignedException::class.java)
        }

        @Test
        @DisplayName("고정 중복 등록 (D1) - SCHEDULE_CONFLICT")
        fun createSchedule_fixedDuplicate() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existingSchedule = createSchedule(id = 50L, workingCategory3 = WorkingCategory3.FIXED)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns listOf(existingSchedule)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("고정과 격고/순회 공존 불가 (D2/D3) - SCHEDULE_CONFLICT")
        fun createSchedule_fixedWithOtherTypes() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existingSchedule = createSchedule(id = 50L, workingCategory3 = WorkingCategory3.ALTERNATE)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns listOf(existingSchedule)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("격고 3건 초과 (D4) - SCHEDULE_CONFLICT")
        fun createSchedule_alternateExceedsLimit() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existing1 = createSchedule(id = 50L, workingCategory3 = WorkingCategory3.ALTERNATE)
            val existing2 = createSchedule(id = 51L, workingCategory3 = WorkingCategory3.ALTERNATE)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.ALTERNATE,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns listOf(existing1, existing2)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("순회+격고1 존재, 격고 추가 - SCHEDULE_CONFLICT")
        fun createSchedule_patrolAndAlternateExist_alternateBlocked() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existingPatrol = createSchedule(id = 50L, workingCategory3 = WorkingCategory3.PATROL)
            val existingAlternate = createSchedule(id = 51L, workingCategory3 = WorkingCategory3.ALTERNATE)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.ALTERNATE,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns listOf(existingPatrol, existingAlternate)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
                .hasMessageContaining("순회 일정이 존재하므로 격고는 1건만 등록 가능합니다")
        }

        @Test
        @DisplayName("순회만 존재, 격고 추가 - 성공")
        fun createSchedule_patrolOnly_alternateAllowed() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val account = createAccount(sfid = "ACC_SFID_001")
            val existingPatrol = createSchedule(id = 50L, workingCategory3 = WorkingCategory3.PATROL)
            val savedSchedule = createSchedule(id = 100L)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.ALTERNATE,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns listOf(existingPatrol)
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns savedSchedule

            // When
            val result = service.createSchedule(currentEmployeeFixture, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
        }

        @Test
        @DisplayName("격고1만 존재(순회 없음), 격고 추가 - 성공")
        fun createSchedule_alternateOnly_secondAlternateAllowed() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val account = createAccount(sfid = "ACC_SFID_001")
            val existingAlternate = createSchedule(id = 50L, workingCategory3 = WorkingCategory3.ALTERNATE)
            val savedSchedule = createSchedule(id = 100L)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.ALTERNATE,
                accountId = 1
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)) } returns listOf(existingAlternate)
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns savedSchedule

            // When
            val result = service.createSchedule(currentEmployeeFixture, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
        }

        @Test
        @DisplayName("근무+거래처 없음 생성 - ACCOUNT_REQUIRED")
        fun createSchedule_workTypeWorkWithoutAccount() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                accountId = null
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)

            // When & Then
            assertThatThrownBy { service.createSchedule(principalOf(employee), request) }
                .isInstanceOf(TeamScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("연차+거래처 없음 생성 - 정상 허용")
        fun createSchedule_leaveTypeWithoutAccount_success() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val savedSchedule = createSchedule(id = 100L, workingType = WorkingType.ANNUAL_LEAVE, workingCategory1 = null, workingCategory3 = null, accountId = null)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.ANNUAL_LEAVE,
                accountId = null
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns savedSchedule

            // When
            val result = service.createSchedule(principalOf(leader), request)

            // Then
            assertThat(result.id).isEqualTo(100L)
        }

        @Test
        @DisplayName("대휴+거래처 없음 생성 - 정상 허용")
        fun createSchedule_substituteLeaveWithoutAccount_success() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val savedSchedule = createSchedule(id = 101L, workingType = WorkingType.ALT_HOLIDAY, workingCategory1 = null, workingCategory3 = null, accountId = null)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = WorkingType.ALT_HOLIDAY,
                accountId = null
            )

            every { employeeRepository.findByEmployeeCode("20030001") } returns Optional.of(employee)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns savedSchedule

            // When
            val result = service.createSchedule(principalOf(leader), request)

            // Then
            assertThat(result.id).isEqualTo(101L)
        }

        @Test
        @DisplayName("미존재 사원 등록 - NOT_FOUND")
        fun createSchedule_employeeNotFound() {
            // Given
            val request = TeamScheduleCreateRequest(
                employeeCode = "NONEXISTENT",
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK
            )

            every { employeeRepository.findByEmployeeCode("NONEXISTENT") } returns Optional.empty()

            // When & Then
            assertThatThrownBy { service.createSchedule(currentEmployeeFixture, request) }
                .isInstanceOf(TeamScheduleEmployeeNotFoundException::class.java)
        }
    }

    // ========== updateSchedule ==========

    @Nested
    @DisplayName("updateSchedule - 일정 수정")
    inner class UpdateScheduleTests {

        @Test
        @DisplayName("정상 수정 - 거래처 변경")
        fun updateSchedule_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )
            val newAccount = createAccount(id = 2, sfid = "ACC_NEW")

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 2
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)
            every { accountRepository.findById(2) } returns Optional.of(newAccount)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then
            assertThat(schedule.account?.id).isEqualTo(2)
        }

        @Test
        @DisplayName("미존재 일정 수정 - NOT_FOUND")
        fun updateSchedule_notFound() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK
            )

            every { teamMemberScheduleRepository.findById(999L) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { service.updateSchedule(principalOf(currentUser), 999L, request) }
                .isInstanceOf(TeamScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("진열마스터 연결 일정 수정 (일반 사용자) - DISPLAY_MASTER_LINK_CONSTRAINT")
        fun updateSchedule_displayMasterLinked_forbidden() {
            // UC-13 FK 매칭 — schedule.displayWorkSchedule 가 진열마스터를 가리키는 일정은 LEADER 수정 차단
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                accountId = 1,
                displayWorkSchedule = dummyDisplayMaster()
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.updateSchedule(principalOf(currentUser), 100L, request) }
                .isInstanceOf(TeamScheduleDisplayMasterLinkException::class.java)
        }

        @Test
        @DisplayName("진열마스터 연결 일정 수정 (시스템관리자) - 수정 성공")
        fun updateSchedule_displayMasterLinked_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = null)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then (no exception thrown)
        }

        @Test
        @DisplayName("진열마스터 연결 일정 수정 (영업지원실) - 수정 성공")
        fun updateSchedule_displayMasterLinked_salesSupport_success() {
            // Given
            val salesSupport = createEmployee(id = 10L, role = null)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then (no exception thrown)
        }

        @Test
        @DisplayName("과거 진열 일정 날짜 변경 - PAST_DATE_CHANGE_NOT_ALLOWED")
        fun updateSchedule_pastDate_displaySchedule_dateChange() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val yesterday = LocalDate.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = yesterday,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS).toString(),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )
            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.updateSchedule(currentEmployeeFixture, 100L, request) }
                .isInstanceOf(TeamSchedulePastDateChangeException::class.java)
        }

        @Test
        @DisplayName("과거 근무 일정 날짜 변경 (카테고리 null) - PAST_DATE_CHANGE_NOT_ALLOWED")
        fun updateSchedule_pastDate_nullCategory_dateChange() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val threeDaysAgo = LocalDate.now().minus(3, java.time.temporal.ChronoUnit.DAYS)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = threeDaysAgo,
                workingType = WorkingType.WORK,
                workingCategory1 = null,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS).toString(),
                workingType = WorkingType.WORK,
                accountId = 1
            )
            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.updateSchedule(principalOf(currentUser), 100L, request) }
                .isInstanceOf(TeamSchedulePastDateChangeException::class.java)
        }

        @Test
        @DisplayName("과거 행사 일정 날짜 변경 - 수정 성공 (행사 예외)")
        fun updateSchedule_pastDate_eventSchedule_dateChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val yesterday = LocalDate.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
            val tomorrow = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = yesterday,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = tomorrow.toString(),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                accountId = 1
            )
            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then
            assertThat(schedule.workingDate).isEqualTo(tomorrow)
        }

        @Test
        @DisplayName("과거 일자 비날짜 필드 수정 - 수정 성공")
        fun updateSchedule_pastDate_nonDateFieldChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val yesterday = LocalDate.now().minus(1, java.time.temporal.ChronoUnit.DAYS)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = yesterday,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = yesterday.toString(),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                accountId = 1
            )
            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then
            assertThat(schedule.workingCategory1?.displayName).isEqualTo("행사")
        }

        @Test
        @DisplayName("오늘 일자 날짜 변경 - 수정 성공")
        fun updateSchedule_todayDate_dateChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val today = LocalDate.now()
            val tomorrow = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = today,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = tomorrow.toString(),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )
            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, tomorrow) } returns emptyList()

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then
            assertThat(schedule.workingDate).isEqualTo(tomorrow)
        }

        @Test
        @DisplayName("미래 일자 날짜 변경 - 수정 성공")
        fun updateSchedule_futureDate_dateChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val tomorrow = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
            val dayAfter = LocalDate.now().plus(2, java.time.temporal.ChronoUnit.DAYS)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = tomorrow,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = dayAfter.toString(),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )
            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, dayAfter) } returns emptyList()

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then
            assertThat(schedule.workingDate).isEqualTo(dayAfter)
        }

        @Test
        @DisplayName("비진열 일정 수정 (일반 사용자) - 수정 성공")
        fun updateSchedule_nonDisplay_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                accountId = 1
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then (no exception, no display master check)
        }

        @Test
        @DisplayName("근무→근무 수정 거래처 미지정 - ACCOUNT_REQUIRED")
        fun updateSchedule_workTypeWorkWithoutAccount() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                accountId = null
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.updateSchedule(currentEmployeeFixture, 100L, request) }
                .isInstanceOf(TeamScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("연차→근무 수정 거래처 미지정 - ACCOUNT_REQUIRED")
        fun updateSchedule_leaveToWorkWithoutAccount() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.ANNUAL_LEAVE,
                workingCategory1 = null,
                workingCategory3 = null,
                accountId = null
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.WORK,
                accountId = null
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.updateSchedule(currentEmployeeFixture, 100L, request) }
                .isInstanceOf(TeamScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("근무→연차 수정 거래처 제거 - 정상 허용")
        fun updateSchedule_workToLeaveWithoutAccount_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory3 = WorkingCategory3.FIXED,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = WorkingType.ANNUAL_LEAVE,
                accountId = null
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.updateSchedule(currentEmployeeFixture, 100L, request)

            // Then
            assertThat(schedule.workingType?.displayName).isEqualTo("연차")
            assertThat(schedule.account).isNull()
        }
    }

    // ========== deleteSchedule ==========

    @Nested
    @DisplayName("deleteSchedule - 일정 삭제")
    inner class DeleteScheduleTests {

        @Test
        @DisplayName("정상 삭제 - 조장 계정으로 삭제 성공")
        fun deleteSchedule_success() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(id = 100L)

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.deleteSchedule(principalOf(leader), 100L)

            // Then
            verify { teamMemberScheduleRepository.delete(schedule) }
        }

        @Test
        @DisplayName("지점장 삭제 시도 - FORBIDDEN")
        fun deleteSchedule_forbiddenForBranchManager() {
            // Given
            val branchManager = createEmployee(id = 10L, role = AppAuthority.BRANCH_MANAGER)

            // When & Then
            assertThatThrownBy { service.deleteSchedule(principalOf(branchManager), 100L) }
                .isInstanceOf(TeamScheduleDeleteForbiddenException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.delete(any()) }
        }

        @Test
        @DisplayName("미존재 일정 삭제 - NOT_FOUND")
        fun deleteSchedule_notFound() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            every { teamMemberScheduleRepository.findById(999L) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { service.deleteSchedule(principalOf(leader), 999L) }
                .isInstanceOf(TeamScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("근무등록 완료 일정 삭제 (일반 사용자) - WORK_REPORT_DELETE_CONSTRAINT")
        fun deleteSchedule_workReportCompleted_forbidden() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(id = 100L, commuteLogSfid = "CL001")

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.deleteSchedule(principalOf(leader), 100L) }
                .isInstanceOf(TeamScheduleWorkReportDeleteException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.delete(any()) }
        }

        @Test
        @DisplayName("근무등록 완료 일정 삭제 (시스템관리자) - 삭제 성공")
        fun deleteSchedule_workReportCompleted_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = null)
            val schedule = createSchedule(id = 100L, commuteLogSfid = "CL001")

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.deleteSchedule(principalOf(admin, profileName = "시스템 관리자"), 100L)

            // Then
            verify { teamMemberScheduleRepository.delete(schedule) }
        }

        @Test
        @DisplayName("미등록 일정 삭제 (일반 사용자) - 삭제 성공")
        fun deleteSchedule_noWorkReport_success() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(id = 100L, commuteLogSfid = null)

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.deleteSchedule(principalOf(leader), 100L)

            // Then
            verify { teamMemberScheduleRepository.delete(schedule) }
        }

        @Test
        @DisplayName("진열마스터 연결 일정 삭제 (일반 사용자) - DISPLAY_MASTER_LINK_CONSTRAINT")
        fun deleteSchedule_displayMasterLinked_forbidden() {
            // UC-13 FK 매칭 — schedule.displayWorkSchedule 가 진열마스터를 가리키는 일정은 LEADER 삭제 차단
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val schedule = createSchedule(
                id = 100L, commuteLogSfid = null,
                workingCategory1 = WorkingCategory1.DISPLAY,
                displayWorkSchedule = dummyDisplayMaster()
            )

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When & Then
            assertThatThrownBy { service.deleteSchedule(principalOf(leader), 100L) }
                .isInstanceOf(TeamScheduleDisplayMasterLinkException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.delete(any()) }
        }

        @Test
        @DisplayName("진열마스터 연결 일정 삭제 (시스템관리자) - 삭제 성공")
        fun deleteSchedule_displayMasterLinked_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = null)
            val schedule = createSchedule(id = 100L, commuteLogSfid = null, workingCategory1 = WorkingCategory1.DISPLAY)

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.deleteSchedule(principalOf(admin), 100L)

            // Then
            verify { teamMemberScheduleRepository.delete(schedule) }
        }

        @Test
        @DisplayName("진열마스터 연결 일정 삭제 (영업지원실) - 삭제 성공")
        fun deleteSchedule_displayMasterLinked_salesSupport_success() {
            // Given
            val salesSupport = createEmployee(id = 10L, role = null)
            val schedule = createSchedule(id = 100L, commuteLogSfid = null, workingCategory1 = WorkingCategory1.DISPLAY)

            every { teamMemberScheduleRepository.findById(100L) } returns Optional.of(schedule)

            // When
            service.deleteSchedule(principalOf(salesSupport), 100L)

            // Then
            verify { teamMemberScheduleRepository.delete(schedule) }
        }
    }

    // ========== massDelete (Spec #691 P1-B) ==========

    @Nested
    @DisplayName("massDelete - 일정 다건 삭제 (Spec #691 — legacy MassDeleteTmScheduleController 동등)")
    inner class MassDeleteTests {

        @Test
        @DisplayName("정상 일괄 삭제 + MFEIS batch refresh (employeeId × accountId × YearMonth groupBy)")
        fun massDelete_success() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            // 동일 (employee=11, account=21, YearMonth=2026-05) 그룹 — s100 + s101 → refresh 1회만 (Q4 옵션 1 검증)
            val s100 = createSchedule(id = 100L, employeeId = 11L, accountId = 21, workingDate = java.time.LocalDate.of(2026, 5, 10))
            val s101 = createSchedule(id = 101L, employeeId = 11L, accountId = 21, workingDate = java.time.LocalDate.of(2026, 5, 15))
            // 다른 그룹 (employee=12, account=22, YearMonth=2026-05) → refresh 1회
            val s102 = createSchedule(id = 102L, employeeId = 12L, accountId = 22, workingDate = java.time.LocalDate.of(2026, 5, 20))

            every { teamMemberScheduleRepository.findAllById(listOf(100L, 101L, 102L)) } returns listOf(s100, s101, s102)

            // When
            val deletedCount = service.massDelete(principalOf(leader), listOf(100L, 101L, 102L))

            // Then
            assertThat(deletedCount).isEqualTo(3)
            verify { teamMemberScheduleRepository.deleteAll(listOf(s100, s101, s102)) }
            // Q4 옵션 1 — 2개 그룹만 refresh 호출 (s100 + s101 동일 그룹 통합)
            verify(exactly = 1) {
                adminMonthlyIntegrationService.refreshIntegration(11L, 21, java.time.YearMonth.of(2026, 5))
            }
            verify(exactly = 1) {
                adminMonthlyIntegrationService.refreshIntegration(12L, 22, java.time.YearMonth.of(2026, 5))
            }
        }

        @Test
        @DisplayName("지점장 호출 - FORBIDDEN (delete 전 즉시 차단)")
        fun massDelete_forbiddenForBranchManager() {
            // Given
            val branchManager = createEmployee(id = 10L, role = AppAuthority.BRANCH_MANAGER)

            // When & Then
            assertThatThrownBy { service.massDelete(principalOf(branchManager), listOf(100L)) }
                .isInstanceOf(TeamScheduleDeleteForbiddenException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.findAllById(any<List<Long>>()) }
            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("100건 초과 - ROW_LIMIT_EXCEEDED (Q1 옵션 1 — legacy 100건 임계값 동등)")
        fun massDelete_rowLimitExceeded() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val ids = (1L..101L).toList()

            // When & Then
            assertThatThrownBy { service.massDelete(principalOf(leader), ids) }
                .isInstanceOf(com.otoki.powersales.schedule.exception.TeamScheduleMassDeleteRowLimitExceededException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.findAllById(any<List<Long>>()) }
        }

        @Test
        @DisplayName("100건 경계 (distinct 후) - 정상 처리")
        fun massDelete_exactly100_success() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val ids = (1L..100L).toList()
            val schedules = ids.map { createSchedule(id = it) }

            every { teamMemberScheduleRepository.findAllById(ids) } returns schedules

            // When
            val deletedCount = service.massDelete(principalOf(leader), ids)

            // Then
            assertThat(deletedCount).isEqualTo(100)
        }

        @Test
        @DisplayName("중복 ids 는 distinct 후 100건 임계 적용")
        fun massDelete_duplicateIdsCollapsedBeforeLimit() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val ids = (1L..50L).toList() + (1L..50L).toList() // 100건이지만 distinct 50건
            val distinct = (1L..50L).toList()
            val schedules = distinct.map { createSchedule(id = it) }

            every { teamMemberScheduleRepository.findAllById(distinct) } returns schedules

            // When
            val deletedCount = service.massDelete(principalOf(leader), ids)

            // Then
            assertThat(deletedCount).isEqualTo(50)
        }

        @Test
        @DisplayName("일부 ids 미존재 - TEAM_SCHEDULE_NOT_FOUND_PARTIAL (legacy 클라이언트 ID 신뢰 회피)")
        fun massDelete_notFoundPartial() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val s100 = createSchedule(id = 100L)
            every { teamMemberScheduleRepository.findAllById(listOf(100L, 101L)) } returns listOf(s100)

            // When & Then
            assertThatThrownBy { service.massDelete(principalOf(leader), listOf(100L, 101L)) }
                .isInstanceOfSatisfying(com.otoki.powersales.schedule.exception.TeamScheduleNotFoundPartialException::class.java) {
                    assertThat(it.missingIds).containsExactly(101L)
                }
            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("Q5 옵션 1 - 1건 가드 fail (출근완료) 시 전체 rollback (delete 미호출, 도메인 예외 throw)")
        fun massDelete_q5OptionOne_anyGuardFail_throwsAndNoDelete() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val s100 = createSchedule(id = 100L, commuteLogSfid = null)
            // s101 은 출근완료 (attendanceLog != null) — leader 는 SYSTEM_ADMIN 아니라 차단
            val s101 = createSchedule(id = 101L, commuteLogSfid = "CL101")

            every { teamMemberScheduleRepository.findAllById(listOf(100L, 101L)) } returns listOf(s100, s101)

            // When & Then
            assertThatThrownBy { service.massDelete(principalOf(leader), listOf(100L, 101L)) }
                .isInstanceOf(TeamScheduleWorkReportDeleteException::class.java)
            // Q5 옵션 1 — 가드 fail 시 deleteAll 미호출 (전체 rollback, legacy allOrNone=true 동등)
            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
            verify(exactly = 0) { adminMonthlyIntegrationService.refreshIntegration(any(), any(), any()) }
        }

        @Test
        @DisplayName("Q5 옵션 1 - 1건 가드 fail (진열마스터 link) 시 전체 rollback")
        fun massDelete_q5OptionOne_displayMasterLinkFail_throwsAndNoDelete() {
            // Given
            val leader = createEmployee(id = 10L, role = AppAuthority.LEADER)
            val s100 = createSchedule(id = 100L, commuteLogSfid = null)
            val s101 = createSchedule(
                id = 101L, commuteLogSfid = null,
                workingCategory1 = WorkingCategory1.DISPLAY,
                displayWorkSchedule = dummyDisplayMaster()
            )

            every { teamMemberScheduleRepository.findAllById(listOf(100L, 101L)) } returns listOf(s100, s101)

            // When & Then
            assertThatThrownBy { service.massDelete(principalOf(leader), listOf(100L, 101L)) }
                .isInstanceOf(TeamScheduleDisplayMasterLinkException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("출근완료 일정 일괄 삭제 (시스템관리자) - 가드 우회 + 삭제 성공")
        fun massDelete_workReportCompleted_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = null)
            val s100 = createSchedule(id = 100L, commuteLogSfid = "CL100")
            val s101 = createSchedule(id = 101L, commuteLogSfid = "CL101")

            every { teamMemberScheduleRepository.findAllById(listOf(100L, 101L)) } returns listOf(s100, s101)

            // When
            val deletedCount = service.massDelete(principalOf(admin, profileName = "시스템 관리자"), listOf(100L, 101L))

            // Then
            assertThat(deletedCount).isEqualTo(2)
            verify { teamMemberScheduleRepository.deleteAll(listOf(s100, s101)) }
        }
    }
}
