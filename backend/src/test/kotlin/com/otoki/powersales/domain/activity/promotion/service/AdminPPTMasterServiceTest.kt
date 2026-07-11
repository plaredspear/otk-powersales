package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkItem
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkValidateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterConfirmByIdsRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterAccountNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterDuplicateException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterEmployeeNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterInvalidDateRangeException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterNotFoundException
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistorySearchResult
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterSearchResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageImpl
import java.io.ByteArrayInputStream
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@DisplayName("AdminPPTMasterService 테스트")
class AdminPPTMasterServiceTest {

    private val pptMasterRepository: PPTMasterRepository = mockk(relaxUnitFun = true)
    private val pptHistoryRepository: PPTHistoryRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()

    private val service: AdminPPTMasterService = AdminPPTMasterService(
        pptMasterRepository = pptMasterRepository,
        pptHistoryRepository = pptHistoryRepository,
        employeeRepository = employeeRepository,
        accountRepository = accountRepository,
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        pptHistoryExcelExporter = PPTHistoryExcelExporter(),
    )

    private lateinit var batchService: PPTMasterBatchService

    @BeforeEach
    fun setUpBatchService() {
        batchService = PPTMasterBatchService(
            pptMasterRepository = pptMasterRepository,
            employeeRepository = employeeRepository,
            adminPPTMasterService = service,
        )
        // 마스터 번호(name) 채번 시퀀스 — 별도 지정 없으면 1 반환 (PM0000001)
        every { pptMasterRepository.getNextNameSeq() } returns 1L
        // 이력 번호(name) 채번 시퀀스 — 별도 지정 없으면 1 반환 (PH0000001)
        every { pptHistoryRepository.getNextNameSeq() } returns 1L
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "12345678",
        name: String = "홍길동",
        professionalPromotionTeam: ProfessionalPromotionTeamType? = null,
        costCenterCode: String = "1100"
    ): Employee {
        val emp = Employee(id = id, employeeCode = employeeCode, name = name)
        emp.costCenterCode = costCenterCode
        emp.orgName = "서울지점"
        emp.status = "재직"
        emp.role = AppAuthority.WOMAN
        emp.professionalPromotionTeam = professionalPromotionTeam
        return emp
    }

    private fun createAccount(
        id: Long = 1L,
        externalKey: String? = "SAP001",
        name: String? = "이마트 강남점"
    ): Account {
        return Account(id = id, externalKey = externalKey, name = name)
    }

    /** 전사 권한 DataScope (지점 스코프 필터 없음) — 기존 전사 조회 테스트 동작 보존용. */
    private fun allBranchesScope(): DataScope =
        DataScope(branchCodes = emptyList(), isAllBranches = true)

    private fun createMaster(
        id: Long = 1L,
        employeeId: Long = 1L,
        accountId: Long = 1,
        teamType: ProfessionalPromotionTeamType = ProfessionalPromotionTeamType.RAMEN_SALE,
        startDate: LocalDate = LocalDate.of(2026, 3, 1),
        endDate: LocalDate? = null,
        isConfirmed: Boolean = true
    ): ProfessionalPromotionTeamMaster {
        return ProfessionalPromotionTeamMaster(
            id = id, employeeId = employeeId, accountId = accountId,
            teamType = teamType, startDate = startDate, endDate = endDate,
            isConfirmed = isConfirmed, branchCode = "1100"
        )
    }

    private fun stubTeamMemberScheduleDelete() {
        every { teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(any(), any()) } returns 0L
    }

    @Nested
    @DisplayName("getMaster - 마스터 상세 조회")
    inner class GetMasterTests {

        @Test
        @DisplayName("성공 - 존재하는 ID -> 마스터 상세 반환")
        fun getMaster_success() {
            val master = createMaster()
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())

            val result = service.getMaster(1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.teamType).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            assertThat(result.employeeName).isEqualTo("홍길동")
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> PPTMasterNotFoundException")
        fun getMaster_notFound() {
            every { pptMasterRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { service.getMaster(999L) }
                .isInstanceOf(PPTMasterNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("createMaster - 마스터 생성")
    inner class CreateMasterTests {

        @Test
        @DisplayName("성공 - 유효한 요청 -> 마스터 생성")
        fun createMaster_success() {
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            val result = service.createMaster(request)

            assertThat(result.teamType).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify(exactly = 0) { pptHistoryRepository.save(any()) }
        }

        @Test
        @DisplayName("성공 - 마스터 번호(name) 채번 적용 (SF AutoNumber PM{0000000} 동등, PM + 7자리)")
        fun createMaster_assignsMasterName() {
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every { pptMasterRepository.getNextNameSeq() } returns 921L
            val saved = slot<ProfessionalPromotionTeamMaster>()
            every { pptMasterRepository.save(capture(saved)) } answers { firstArg() }

            val result = service.createMaster(request)

            // 채번 쿼리 호출 + 저장 엔티티/응답에 PM0000921 set
            verify { pptMasterRepository.getNextNameSeq() }
            assertThat(saved.captured.name).isEqualTo("PM0000921")
            assertThat(result.name).isEqualTo("PM0000921")
        }

        @Test
        @DisplayName("성공 - startDate=오늘, 확정 -> 사원 즉시 반영 + 이력 기록")
        fun createMaster_immediateSync() {
            val today = LocalDate.now()
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = today, isConfirmed = true
            )
            val employee = createEmployee()
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            every {
                pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>())
            } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.createMaster(request)

            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify { pptHistoryRepository.save(any()) }
        }

        @Test
        @DisplayName("성공 - 기존 유효 마스터 자동 종료")
        fun createMaster_autoTerminate() {
            val existingMaster = createMaster(id = 10L, teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns listOf(existingMaster)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            service.createMaster(request)

            assertThat(existingMaster.endDate).isEqualTo(LocalDate.of(2026, 3, 31))
        }

        @Test
        @DisplayName("실패 - startDate > endDate -> PPTMasterInvalidDateRangeException")
        fun createMaster_invalidDateRange() {
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 5, 1), endDate = LocalDate.of(2026, 4, 1)
            )
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())

            assertThatThrownBy { service.createMaster(request) }
                .isInstanceOf(PPTMasterInvalidDateRangeException::class.java)
        }

        @Test
        @DisplayName("실패 - 사원 미존재 -> PPTMasterEmployeeNotFoundException")
        fun createMaster_employeeNotFound() {
            val request = PPTMasterCreateRequest(
                employeeId = 999L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1)
            )
            every { employeeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { service.createMaster(request) }
                .isInstanceOf(PPTMasterEmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 거래처 미존재 -> PPTMasterAccountNotFoundException")
        fun createMaster_accountNotFound() {
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 999, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1)
            )
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(999) } returns Optional.empty()

            assertThatThrownBy { service.createMaster(request) }
                .isInstanceOf(PPTMasterAccountNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 중복 유효 마스터 -> PPTMasterDuplicateException")
        fun createMaster_duplicate() {
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1)
            )
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns listOf(createMaster())

            assertThatThrownBy { service.createMaster(request) }
                .isInstanceOf(PPTMasterDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("updateMaster - 마스터 수정")
    inner class UpdateMasterTests {

        @Test
        @DisplayName("성공 - 유효한 수정 -> 수정된 마스터 반환")
        fun updateMaster_success() {
            val master = createMaster()
            val request = PPTMasterUpdateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            val result = service.updateMaster(1L, request)

            assertThat(result.teamType).isEqualTo(ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)
        }

        @Test
        @DisplayName("성공 - accountId 변경 -> 거래처 변경 반영")
        fun updateMaster_changeAccountId() {
            val master = createMaster(accountId = 1)
            val newAccount = createAccount(id = 2, externalKey = "SAP002", name = "홈플러스 강남점")
            val request = PPTMasterUpdateRequest(
                employeeId = 1L, accountId = 2, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(2) } returns Optional.of(newAccount)
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            val result = service.updateMaster(1L, request)

            assertThat(master.accountId).isEqualTo(2)
            assertThat(result.accountCode).isEqualTo("SAP002")
        }

        @Test
        @DisplayName("성공 - 사원 변경 -> employee_id + branch_code 가 새 사원 소속 지점으로 재계산 (SF BranchName__c formula 동등)")
        fun updateMaster_changeEmployee_recalculatesBranchCode() {
            // 기존 마스터: 사원 1L (지점 1100)
            val master = createMaster(employeeId = 1L)
            // 새 사원 2L (지점 7700)
            val newEmployee = createEmployee(id = 2L, employeeCode = "87654321", name = "김영희", costCenterCode = "7700")
            val request = PPTMasterUpdateRequest(
                employeeId = 2L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { employeeRepository.findById(2L) } returns Optional.of(newEmployee)
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(2L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            service.updateMaster(1L, request)

            // 사원 변경 반영 + branch_code 가 새 사원의 cost_center_code 로 재계산
            assertThat(master.employeeId).isEqualTo(2L)
            assertThat(master.branchCode).isEqualTo("7700")
        }

        @Test
        @DisplayName("성공 - update 시 동일 사원의 다른 teamType 유효 마스터 자동 종료 (UC-05)")
        fun updateMaster_autoTerminate() {
            val master = createMaster(id = 1L, teamType = ProfessionalPromotionTeamType.RAMEN_SALE)
            val existingOther = createMaster(
                id = 10L,
                teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED,
                endDate = null
            )
            val request = PPTMasterUpdateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every {
                pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L)
            } returns listOf(master, existingOther)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            service.updateMaster(1L, request)

            // 본 레코드 자신은 종료되지 않고 다른 마스터만 종료일 자동 set
            assertThat(master.endDate).isNull()
            assertThat(existingOther.endDate).isEqualTo(LocalDate.of(2026, 3, 31))
        }

        @Test
        @DisplayName("실패 - accountId 변경 시 중복 -> PPTMasterDuplicateException")
        fun updateMaster_changeAccountId_duplicate() {
            val master = createMaster(accountId = 1)
            val newAccount = createAccount(id = 2, externalKey = "SAP002", name = "홈플러스 강남점")
            val request = PPTMasterUpdateRequest(
                employeeId = 1L, accountId = 2, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { accountRepository.findById(2) } returns Optional.of(newAccount)
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns listOf(createMaster(id = 2L, accountId = 2))

            assertThatThrownBy { service.updateMaster(1L, request) }
                .isInstanceOf(PPTMasterDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteMaster - 마스터 삭제")
    inner class DeleteMasterTests {

        @Test
        @DisplayName("성공 - 다른 유효 마스터 없음 -> 사원 미배정(null)으로 복귀")
        fun deleteMaster_revertToDefault() {
            val master = createMaster()
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { pptMasterRepository.findValidMastersByEmployeeId(1L, LocalDate.now()) } returns emptyList()
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.deleteMaster(1L)

            verify { pptMasterRepository.delete(master) }
            assertThat(employee.professionalPromotionTeam).isNull()
        }

        @Test
        @DisplayName("성공 - 다른 유효 마스터 존재 -> 사원 변경 없음")
        fun deleteMaster_otherMastersExist() {
            val master = createMaster(id = 1L)
            val otherMaster = createMaster(id = 2L, teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)

            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every {
                pptMasterRepository.findValidMastersByEmployeeId(1L, LocalDate.now())
            } returns listOf(otherMaster)

            service.deleteMaster(1L)

            verify { pptMasterRepository.delete(master) }
            verify(exactly = 0) { employeeRepository.save(any()) }
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> PPTMasterNotFoundException")
        fun deleteMaster_notFound() {
            every { pptMasterRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { service.deleteMaster(999L) }
                .isInstanceOf(PPTMasterNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getMasters - 마스터 목록 조회")
    inner class GetMastersTests {

        @Test
        @DisplayName("성공 - 목록 조회 -> 페이지 응답")
        fun getMasters_success() {
            val master = createMaster()
            val searchResult = PPTMasterSearchResult(master, "12345678", "홍길동", "SAP001", "이마트 강남점", branchName = "강남지점", employeeStatus = "재직", employeeAppLoginActive = true, employeeEndDate = null, accountType = null)
            val page = PageImpl(listOf(searchResult), PageRequest.of(0, 20), 1)

            every {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.getMasters(allBranchesScope(), null, null, null, null, true, PageRequest.of(0, 20))

            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("지점 스코프 - 본인 지점 권한 -> employee.costCenterCode IN 필터로 조회")
        fun getMasters_branchScoped() {
            val page = PageImpl(emptyList<PPTMasterSearchResult>(), PageRequest.of(0, 20), 0)
            every {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            } returns page

            // 본인 지점 "3233" 단일 권한 (전사 아님)
            val scope = DataScope(branchCodes = listOf("3233"), isAllBranches = false)
            service.getMasters(scope, null, null, null, null, true, PageRequest.of(0, 20))

            // branchCodeFilter 인자(4번째)에 본인 지점 코드가 전달되어야 한다
            verify {
                pptMasterRepository.searchMasters(any(), any(), any(), listOf("3233"), any(), any(), any())
            }
        }

        @Test
        @DisplayName("지점 스코프 - 권한 밖 지점 요청(NoAccess) -> 쿼리 없이 빈 목록")
        fun getMasters_noAccess() {
            // 본인 지점은 "3233" 인데 "9999" 지점 요청 -> NoAccess
            val scope = DataScope(branchCodes = listOf("3233"), isAllBranches = false)
            val result = service.getMasters(scope, null, null, null, "9999", true, PageRequest.of(0, 20))

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
            verify(exactly = 0) {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("confirmByIds - 선택 레코드 일괄 확정 (UC-12)")
    inner class ConfirmByIdsTests {

        @Test
        @DisplayName("성공 - 미확정 마스터를 확정으로 변경하고 확정 카운트 반환")
        fun confirmByIds_success() {
            val master1 = createMaster(id = 1L, isConfirmed = false)
            val master2 = createMaster(id = 2L, isConfirmed = false)
            every { pptMasterRepository.findAllById(listOf(1L, 2L)) } returns listOf(master1, master2)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            val response = service.confirmByIds(PPTMasterConfirmByIdsRequest(ids = listOf(1L, 2L)))

            assertThat(response.confirmedCount).isEqualTo(2)
            assertThat(response.skippedCount).isEqualTo(0)
            assertThat(master1.isConfirmed).isTrue
            assertThat(master2.isConfirmed).isTrue
        }

        @Test
        @DisplayName("성공 - 이미 확정된 레코드는 skip 카운트로 집계")
        fun confirmByIds_skipAlreadyConfirmed() {
            val master1 = createMaster(id = 1L, isConfirmed = false)
            val master2 = createMaster(id = 2L, isConfirmed = true)
            every { pptMasterRepository.findAllById(listOf(1L, 2L)) } returns listOf(master1, master2)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            val response = service.confirmByIds(PPTMasterConfirmByIdsRequest(ids = listOf(1L, 2L)))

            assertThat(response.confirmedCount).isEqualTo(1)
            assertThat(response.skippedCount).isEqualTo(1)
            assertThat(master1.isConfirmed).isTrue
        }

        @Test
        @DisplayName("성공 - 시작일=오늘 + 확정 변경 -> 직원 행사조 즉시 갱신")
        fun confirmByIds_immediateSyncWhenStartDateIsToday() {
            val today = LocalDate.now()
            val master = createMaster(id = 1L, startDate = today, isConfirmed = false)
            val employee = createEmployee(professionalPromotionTeam = null)
            every { pptMasterRepository.findAllById(listOf(1L)) } returns listOf(master)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            every {
                pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>())
            } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.confirmByIds(PPTMasterConfirmByIdsRequest(ids = listOf(1L)))

            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify { pptHistoryRepository.save(any()) }
            verify { teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(1L, today) }
        }

        @Test
        @DisplayName("성공 - 시작일이 미래인 레코드 -> 직원 갱신 호출 안 됨")
        fun confirmByIds_skipImmediateSyncWhenFuture() {
            val futureDate = LocalDate.now().plus(7, ChronoUnit.DAYS)
            val master = createMaster(id = 1L, startDate = futureDate, isConfirmed = false)
            every { pptMasterRepository.findAllById(listOf(1L)) } returns listOf(master)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            service.confirmByIds(PPTMasterConfirmByIdsRequest(ids = listOf(1L)))

            assertThat(master.isConfirmed).isTrue
            verify(exactly = 0) { employeeRepository.save(any()) }
            verify(exactly = 0) { pptHistoryRepository.save(any()) }
        }

        @Test
        @DisplayName("성공 - 요청 id 중 존재하지 않는 id 는 skipped 로 집계")
        fun confirmByIds_skipNotFound() {
            val master = createMaster(id = 1L, isConfirmed = false)
            // 1, 2 요청 / 1 만 조회됨 (2 는 미존재)
            every { pptMasterRepository.findAllById(listOf(1L, 2L)) } returns listOf(master)
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }

            val response = service.confirmByIds(PPTMasterConfirmByIdsRequest(ids = listOf(1L, 2L)))

            assertThat(response.confirmedCount).isEqualTo(1)
            assertThat(response.skippedCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("confirmBulk - 엑셀 일괄 등록 (UC)")
    inner class ConfirmBulkTests {

        @Test
        @DisplayName("성공 - 일괄 등록 각 건에 마스터 번호(name) 채번 적용")
        fun confirmBulk_assignsMasterName() {
            val emp1 = createEmployee(id = 1L, employeeCode = "11111111")
            val emp2 = createEmployee(id = 2L, employeeCode = "22222222")
            val acc = createAccount(id = 1, externalKey = "SAP001")
            val request = PPTMasterBulkValidateRequest(
                items = listOf(
                    PPTMasterBulkItem("11111111", "SAP001", ProfessionalPromotionTeamType.RAMEN_SALE, LocalDate.of(2026, 4, 1)),
                    PPTMasterBulkItem("22222222", "SAP001", ProfessionalPromotionTeamType.RAMEN_SALE, LocalDate.of(2026, 4, 1))
                )
            )
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp1, emp2)
            every { accountRepository.findByExternalKeyIn(any()) } returns listOf(acc)
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(any()) } returns emptyList()
            // 두 건 채번: PM0000921, PM0000922
            every { pptMasterRepository.getNextNameSeq() } returnsMany listOf(921L, 922L)
            val saved = mutableListOf<ProfessionalPromotionTeamMaster>()
            every { pptMasterRepository.save(capture(saved)) } answers { firstArg() }

            val response = service.confirmBulk(request)

            assertThat(response.createdCount).isEqualTo(2)
            // save 캡처분 중 신규 생성 2건의 name 확인 (autoTerminate 저장분은 없음)
            val names = saved.mapNotNull { it.name }
            assertThat(names).containsExactlyInAnyOrder("PM0000921", "PM0000922")
        }
    }

    @Nested
    @DisplayName("exportToExcel - 마스터 데이터 엑셀 다운로드 (UC-11)")
    inner class ExportToExcelTests {

        @Test
        @DisplayName("성공 - 검색 조건에 맞는 마스터를 xlsx 로 반환")
        fun exportToExcel_success() {
            val master = createMaster()
            val searchResult = PPTMasterSearchResult(master, "12345678", "홍길동", "SAP001", "이마트 강남점", branchName = "강남지점", employeeStatus = "재직", employeeAppLoginActive = true, employeeEndDate = null, accountType = null)
            val page = PageImpl(listOf(searchResult), PageRequest.of(0, 50_000), 1)
            every {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.exportToExcel(allBranchesScope(), null, null, null, null, true)

            // xlsx 파일 시그니처 (PK\x03\x04 — ZIP 형식) 확인
            assertThat(result.bytes).isNotEmpty
            assertThat(result.bytes[0]).isEqualTo(0x50.toByte()) // 'P'
            assertThat(result.bytes[1]).isEqualTo(0x4B.toByte()) // 'K'
            assertThat(result.filename).startsWith("전문행사조마스터_").endsWith(".xlsx")
        }

        @Test
        @DisplayName("지점 스코프 - 권한 밖 지점 요청(NoAccess) -> 쿼리 없이 헤더만 빈 xlsx")
        fun exportToExcel_noAccess() {
            val scope = DataScope(branchCodes = listOf("3233"), isAllBranches = false)
            val result = service.exportToExcel(scope, null, null, null, "9999", true)

            assertThat(result.bytes).isNotEmpty // 헤더 행만 있는 빈 엑셀
            verify(exactly = 0) {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            }
        }

        @Test
        @DisplayName("성공 - 결과 0건 -> 헤더만 있는 빈 xlsx 반환")
        fun exportToExcel_empty() {
            val page = PageImpl<PPTMasterSearchResult>(emptyList(), PageRequest.of(0, 50_000), 0)
            every {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.exportToExcel(allBranchesScope(), null, null, null, null, true)

            assertThat(result.bytes).isNotEmpty
        }
    }

    @Nested
    @DisplayName("syncValidMasters - 새벽 배치 동기화")
    inner class SyncValidMastersTests {

        @Test
        @DisplayName("성공 - 유효 마스터와 사원 값 불일치 -> 사원 업데이트")
        fun syncValidMasters_updatesEmployee() {
            val master = createMaster(employeeId = 1L, teamType = ProfessionalPromotionTeamType.RAMEN_SALE)
            val employee = createEmployee(professionalPromotionTeam = null)

            every { pptMasterRepository.findValidMasters(LocalDate.now()) } returns listOf(master)
            every { employeeRepository.findAllById(listOf(1L)) } returns listOf(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            every {
                pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>())
            } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.syncValidMasters()

            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify { pptHistoryRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("expireMasters - 심야 배치 만료")
    inner class ExpireMastersTests {

        @Test
        @DisplayName("성공 - 종료일 도래 -> 사원 미배정(null)으로 복귀")
        fun expireMasters_revertToDefault() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, endDate = today)
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            every { pptMasterRepository.findExpiringMasters(today) } returns listOf(master)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.expireMasters()

            assertThat(employee.professionalPromotionTeam).isNull()
        }

        @Test
        @DisplayName("legacy Batch_PPTMaster2 동등 - 잔여 유효 마스터가 있어도 만료 사원은 무조건 해제")
        fun expireMasters_revertsEvenWhenOtherValidMasterRemains() {
            val today = LocalDate.now()
            // 오늘 종료되는 마스터 A. 같은 사원에게 다음 달 종료되는 유효 마스터 B 가 남아 있어도
            // 레거시는 잔여 마스터 유무를 확인하지 않고 무조건 사원 팀을 해제한다.
            val expiring = createMaster(employeeId = 1L, endDate = today)
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            every { pptMasterRepository.findExpiringMasters(today) } returns listOf(expiring)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.expireMasters()

            // 잔여 유효 마스터 조회 없이 무조건 해제 (레거시 동작) — sync 배치가 익일 재정합.
            assertThat(employee.professionalPromotionTeam).isNull()
            verify(exactly = 0) { pptMasterRepository.findValidMastersByEmployeeId(any(), any()) }
        }
    }

    @Nested
    @DisplayName("updateEmployeeTeam - 미래 근무일정 삭제")
    inner class UpdateEmployeeTeamFutureScheduleTests {

        @Test
        @DisplayName("성공 - PPT 변경 시 미래 근무일정 삭제 호출됨")
        fun updateEmployeeTeam_deletesFutureSchedules() {
            val today = LocalDate.now()
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = today, isConfirmed = true
            )
            val employee = createEmployee(professionalPromotionTeam = null)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            every {
                pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>())
            } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.createMaster(request)

            verify { teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(1L, today) }
        }

        @Test
        @DisplayName("성공 - 이력 생성 시 name(PH+7자리) 채번 (SF AutoNumber PH{0000000} 정합)")
        fun updateEmployeeTeam_generatesHistoryName() {
            val today = LocalDate.now()
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = today, isConfirmed = true
            )
            val employee = createEmployee(professionalPromotionTeam = null)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            // 이력 채번 시퀀스가 17650 반환 → name = PH0017650 (7자리 zero-pad)
            every { pptHistoryRepository.getNextNameSeq() } returns 17650L
            val historySlot = slot<ProfessionalPromotionTeamHistory>()
            every {
                pptHistoryRepository.save(capture(historySlot))
            } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.createMaster(request)

            verify { pptHistoryRepository.getNextNameSeq() }
            assertThat(historySlot.captured.name).isEqualTo("PH0017650")
        }

        @Test
        @DisplayName("동일 값 변경 없음 - 삭제 호출 안 됨")
        fun updateEmployeeTeam_sameValue_noDelete() {
            val today = LocalDate.now()
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = today, isConfirmed = true
            )
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            every {
                pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>())
            } answers { firstArg() }
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            every {
                pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>())
            } answers { firstArg() }

            service.createMaster(request)

            verify(exactly = 0) {
                teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(any(), any())
            }
        }

        @Test
        @DisplayName("성공 - 마스터 삭제 시 일반 복귀 → 미래 근무일정 삭제 호출됨")
        fun deleteMaster_revertToDefault_deletesFutureSchedules() {
            val today = LocalDate.now()
            val master = createMaster()
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { pptMasterRepository.findValidMastersByEmployeeId(1L, today) } returns emptyList()
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.deleteMaster(1L)

            verify { teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(1L, today) }
        }

        @Test
        @DisplayName("성공 - 배치 동기화 시 PPT 변경 → 미래 근무일정 삭제 호출됨")
        fun syncValidMasters_deletesFutureSchedules() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, teamType = ProfessionalPromotionTeamType.RAMEN_SALE)
            val employee = createEmployee(professionalPromotionTeam = null)

            every { pptMasterRepository.findValidMasters(today) } returns listOf(master)
            every { employeeRepository.findAllById(listOf(1L)) } returns listOf(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            every {
                pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>())
            } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.syncValidMasters()

            verify { teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(1L, today) }
        }

        @Test
        @DisplayName("성공 - 배치 만료 시 일반 복귀 → 미래 근무일정 삭제 호출됨")
        fun expireMasters_deletesFutureSchedules() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, endDate = today)
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            every { pptMasterRepository.findExpiringMasters(today) } returns listOf(master)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.expireMasters()

            verify { teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(1L, today) }
        }
    }

    @Nested
    @DisplayName("getAllHistory - 전 사원 시간순 이력 조회")
    inner class GetAllHistoryTests {

        @Test
        @DisplayName("성공 - 필터 없이 호출 → repository searchHistories 호출 + 응답 사원/거래처 컨텍스트 정상")
        fun getAllHistory_noFilter_success() {
            val history = ProfessionalPromotionTeamHistory(
                id = 1L,
                name = "PH0000001",
                employeeId = 1L,
                masterId = 50L,
                oldValue = ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
            )
            val projection = PPTHistorySearchResult(
                history = history,
                employeeName = "홍길동",
                employeeCode = "12345678",
                orgName = "서울지점",
                accountId = 55L,
                accountCode = "SAP001",
                accountName = "이마트 강남점",
            )
            val page = PageImpl(listOf(projection), PageRequest.of(0, 20), 1)

            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.getAllHistory(allBranchesScope(), null, null, null, null, null, null, PageRequest.of(0, 20))

            assertThat(result.content).hasSize(1)
            val row = result.content[0]
            assertThat(row.name).isEqualTo("PH0000001")
            assertThat(row.employeeId).isEqualTo(1L)
            assertThat(row.employeeName).isEqualTo("홍길동")
            assertThat(row.employeeCode).isEqualTo("12345678")
            assertThat(row.orgName).isEqualTo("서울지점")
            assertThat(row.oldValue).isEqualTo(ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING)
            assertThat(row.newValue).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            // 원인 마스터(masterId) 거래처가 응답에 적재된다.
            assertThat(row.accountCode).isEqualTo("SAP001")
            assertThat(row.accountName).isEqualTo("이마트 강남점")
        }

        @Test
        @DisplayName("성공 - teamType 표시명 → enum 변환하여 repository 호출")
        fun getAllHistory_teamTypeDisplayName_converted() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getAllHistory(allBranchesScope(), null, null, "라면세일조", null, null, null, PageRequest.of(0, 20))

            verify {
                pptHistoryRepository.searchHistories(
                    any(), any(),
                    ProfessionalPromotionTeamType.RAMEN_SALE, eq(false),
                    any(), any(), any(), any()
                )
            }
        }

        @Test
        @DisplayName("성공 - teamType '일반' → teamTypeGeneral=true 로 repository 호출 (미지정 이력만 필터)")
        fun getAllHistory_generalTeamType_generalFlag() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getAllHistory(allBranchesScope(), null, null, "일반", null, null, null, PageRequest.of(0, 20))

            verify {
                pptHistoryRepository.searchHistories(
                    any(), any(), null, eq(true),
                    any(), any(), any(), any()
                )
            }
        }

        @Test
        @DisplayName("성공 - 잘못된 teamType 문자열 → null 변환 (예외 없음)")
        fun getAllHistory_invalidTeamType_nullConverted() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getAllHistory(allBranchesScope(), null, null, "잘못된값", null, null, null, PageRequest.of(0, 20))

            verify {
                pptHistoryRepository.searchHistories(
                    any(), any(), null, eq(false),
                    any(), any(), any(), any()
                )
            }
        }

        @Test
        @DisplayName("성공 - 사원/거래처 lookup 결과가 null 인 row → 컨텍스트 필드 null (masterId 없는 이력)")
        fun getAllHistory_deletedEmployee_nullContext() {
            val history = ProfessionalPromotionTeamHistory(
                id = 99L,
                employeeId = 999L,
                masterId = null,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
            )
            val projection = PPTHistorySearchResult(
                history = history,
                employeeName = null,
                employeeCode = null,
                orgName = null,
                accountId = null,
                accountCode = null,
                accountName = null,
            )
            val page = PageImpl(listOf(projection), PageRequest.of(0, 20), 1)

            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.getAllHistory(allBranchesScope(), null, null, null, null, null, null, PageRequest.of(0, 20))

            assertThat(result.content[0].employeeName).isNull()
            assertThat(result.content[0].employeeCode).isNull()
            assertThat(result.content[0].orgName).isNull()
            // masterId 가 null 인 이력은 거래처도 null.
            assertThat(result.content[0].accountCode).isNull()
            assertThat(result.content[0].accountName).isNull()
        }

        @Test
        @DisplayName("지점 스코프 - 본인 지점 권한 -> employee.costCenterCode IN 필터로 조회")
        fun getAllHistory_branchScoped() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            // 본인 지점 "3233" 단일 권한 (전사 아님)
            val scope = DataScope(branchCodes = listOf("3233"), isAllBranches = false)
            service.getAllHistory(scope, null, null, null, null, null, null, PageRequest.of(0, 20))

            // branchCodeFilter 인자(7번째)에 본인 지점 코드가 전달되어야 한다
            verify {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), listOf("3233"), any())
            }
        }

        @Test
        @DisplayName("지점 스코프 - 접근 가능 지점 없음(NoAccess) -> 쿼리 없이 빈 목록")
        fun getAllHistory_noAccess() {
            // 권한 지점 목록이 비어 전사도 아님 -> NoAccess
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            val result = service.getAllHistory(scope, null, null, null, null, null, null, PageRequest.of(0, 20))

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
            verify(exactly = 0) {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("exportHistoryToExcel - 전문행사조 이력 엑셀 다운로드")
    inner class ExportHistoryToExcelTests {

        @Test
        @DisplayName("성공 - 검색결과 전량을 헤더 9컬럼(거래처명/코드 포함) + 데이터 행으로 출력 + 파일명 패턴")
        fun exportHistory_success() {
            val history = ProfessionalPromotionTeamHistory(
                id = 1L,
                name = "PH0000001",
                employeeId = 1L,
                masterId = 50L,
                oldValue = ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
            )
            val projection = PPTHistorySearchResult(
                history = history,
                employeeName = "홍길동",
                employeeCode = "12345678",
                orgName = "서울지점",
                accountId = 55L,
                accountCode = "SAP001",
                accountName = "이마트 강남점",
            )
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(projection), PageRequest.of(0, 50_000), 1)

            val result = service.exportHistoryToExcel(allBranchesScope(), null, null, null, null, null, null)

            assertThat(result.filename).startsWith("전문행사조이력_").endsWith(".xlsx")
            val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.sheetName).isEqualTo("전문행사조이력")
            assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("전문행사조 이력번호")
            assertThat(sheet.getRow(0).getCell(6).stringCellValue).isEqualTo("변경 시점")
            assertThat(sheet.getRow(0).getCell(7).stringCellValue).isEqualTo("거래처명")
            assertThat(sheet.getRow(0).getCell(8).stringCellValue).isEqualTo("거래처코드")
            val dataRow = sheet.getRow(1)
            assertThat(dataRow.getCell(0).stringCellValue).isEqualTo("PH0000001")
            assertThat(dataRow.getCell(1).stringCellValue).isEqualTo("서울지점")
            assertThat(dataRow.getCell(2).stringCellValue).isEqualTo("12345678")
            assertThat(dataRow.getCell(3).stringCellValue).isEqualTo("홍길동")
            assertThat(dataRow.getCell(4).stringCellValue)
                .isEqualTo(ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING.displayName)
            assertThat(dataRow.getCell(5).stringCellValue)
                .isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE.displayName)
            assertThat(dataRow.getCell(7).stringCellValue).isEqualTo("이마트 강남점")
            assertThat(dataRow.getCell(8).stringCellValue).isEqualTo("SAP001")
            workbook.close()
        }

        @Test
        @DisplayName("성공 - teamType 표시명 → enum 변환하여 repository 호출 + 50,000 페이지로 전량 조회")
        fun exportHistory_filterAndPageSize() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 50_000), 0)

            service.exportHistoryToExcel(allBranchesScope(), "홍", "123", "라면세일조", null, null, null)

            verify {
                pptHistoryRepository.searchHistories(
                    eq("홍"), eq("123"),
                    ProfessionalPromotionTeamType.RAMEN_SALE, eq(false),
                    any(), any(), any(),
                    match { it.pageSize == 50_000 }
                )
            }
        }

        @Test
        @DisplayName("성공 - teamType '일반' → teamTypeGeneral=true 로 repository 호출 (목록 화면과 동일 해석)")
        fun exportHistory_generalTeamType_generalFlag() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 50_000), 0)

            service.exportHistoryToExcel(allBranchesScope(), null, null, "일반", null, null, null)

            verify {
                pptHistoryRepository.searchHistories(
                    any(), any(), null, eq(true),
                    any(), any(), any(), any()
                )
            }
        }

        @Test
        @DisplayName("지점 스코프 - NoAccess -> 쿼리 없이 헤더만 있는 빈 엑셀")
        fun exportHistory_noAccess() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)

            val result = service.exportHistoryToExcel(scope, null, null, null, null, null, null)

            val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
            assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(0) // 헤더 행만
            workbook.close()
            verify(exactly = 0) {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("이력 masterId - 원인 마스터 FK 기록")
    inner class HistoryMasterIdTests {

        @Test
        @DisplayName("createMaster 즉시 반영 -> 이력에 원인 마스터 id 기록")
        fun createMaster_recordsMasterId() {
            val today = LocalDate.now()
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = today, isConfirmed = true
            )
            val employee = createEmployee(professionalPromotionTeam = null)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(createAccount())
            every {
                pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), any())
            } returns emptyList()
            every { pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L) } returns emptyList()
            // 저장된 마스터에 id=42 부여 (채번 대신 직접 set)
            every { pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()) } answers {
                ProfessionalPromotionTeamMaster(
                    id = 42L, employeeId = 1L, accountId = 1,
                    teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                    startDate = today, isConfirmed = true, branchCode = "1100"
                )
            }
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            val saved = slot<ProfessionalPromotionTeamHistory>()
            every { pptHistoryRepository.save(capture(saved)) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.createMaster(request)

            assertThat(saved.captured.masterId).isEqualTo(42L)
        }

        @Test
        @DisplayName("confirmByIds 즉시 반영 -> 이력에 원인 마스터 id 기록")
        fun confirmByIds_recordsMasterId() {
            val today = LocalDate.now()
            val master = createMaster(id = 7L, startDate = today, isConfirmed = false)
            val employee = createEmployee(professionalPromotionTeam = null)
            every { pptMasterRepository.findAllById(listOf(7L)) } returns listOf(master)
            every { pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()) } answers { firstArg() }
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            val saved = slot<ProfessionalPromotionTeamHistory>()
            every { pptHistoryRepository.save(capture(saved)) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.confirmByIds(PPTMasterConfirmByIdsRequest(ids = listOf(7L)))

            assertThat(saved.captured.masterId).isEqualTo(7L)
        }

        @Test
        @DisplayName("syncValidMasters 배치 -> 이력에 원인 마스터 id 기록")
        fun syncValidMasters_recordsMasterId() {
            val master = createMaster(id = 13L, employeeId = 1L, teamType = ProfessionalPromotionTeamType.RAMEN_SALE)
            val employee = createEmployee(professionalPromotionTeam = null)
            every { pptMasterRepository.findValidMasters(LocalDate.now()) } returns listOf(master)
            every { employeeRepository.findAllById(listOf(1L)) } returns listOf(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            val saved = slot<ProfessionalPromotionTeamHistory>()
            every { pptHistoryRepository.save(capture(saved)) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.syncValidMasters()

            assertThat(saved.captured.masterId).isEqualTo(13L)
        }

        @Test
        @DisplayName("expireMasters 해제 -> 이력 자체 미기록 (masterId 전달 무의미)")
        fun expireMasters_noHistory() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, endDate = today)
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            every { pptMasterRepository.findExpiringMasters(today) } returns listOf(master)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.expireMasters()

            // 해제(→null)는 기존 정책대로 이력을 남기지 않는다.
            verify(exactly = 0) { pptHistoryRepository.save(any()) }
        }

        @Test
        @DisplayName("deleteMaster 해제 -> 이력 자체 미기록")
        fun deleteMaster_noHistory() {
            val master = createMaster()
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every { pptMasterRepository.findValidMastersByEmployeeId(1L, LocalDate.now()) } returns emptyList()
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            service.deleteMaster(1L)

            verify(exactly = 0) { pptHistoryRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("getHistory - 사원 컨텍스트 3 필드 보강")
    inner class GetHistoryEnrichmentTests {

        @Test
        @DisplayName("성공 - getHistory 응답에 name/employeeCode/orgName + 원인 마스터 거래처 포함")
        fun getHistory_includesEmployeeContext() {
            val master = createMaster(employeeId = 1L)
            val history = ProfessionalPromotionTeamHistory(
                id = 1L,
                name = "PH0000001",
                employeeId = 1L,
                masterId = 1L,
                oldValue = null,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE
            )
            val projection = PPTHistorySearchResult(
                history = history,
                employeeName = "홍길동",
                employeeCode = "12345678",
                orgName = "서울지점",
                accountId = 55L,
                accountCode = "SAP001",
                accountName = "이마트 강남점",
            )

            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every {
                pptHistoryRepository.findHistoriesByEmployeeId(1L, any())
            } returns PageImpl(listOf(projection), PageRequest.of(0, 20), 1)

            val result = service.getHistory(1L, PageRequest.of(0, 20))

            assertThat(result.content[0].name).isEqualTo("PH0000001")
            assertThat(result.content[0].employeeName).isEqualTo("홍길동")
            assertThat(result.content[0].employeeCode).isEqualTo("12345678")
            assertThat(result.content[0].orgName).isEqualTo("서울지점")
            assertThat(result.content[0].accountCode).isEqualTo("SAP001")
            assertThat(result.content[0].accountName).isEqualTo("이마트 강남점")
        }
    }
}
