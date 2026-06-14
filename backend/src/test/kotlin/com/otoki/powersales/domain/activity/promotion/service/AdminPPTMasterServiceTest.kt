package com.otoki.powersales.domain.activity.promotion.service

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
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterSearchResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
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
    )

    private lateinit var batchService: PPTMasterBatchService

    @BeforeEach
    fun setUpBatchService() {
        batchService = PPTMasterBatchService(
            pptMasterRepository = pptMasterRepository,
            employeeRepository = employeeRepository,
            adminPPTMasterService = service,
        )
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "12345678",
        name: String = "홍길동",
        professionalPromotionTeam: ProfessionalPromotionTeamType? = null
    ): Employee {
        val emp = Employee(id = id, employeeCode = employeeCode, name = name)
        emp.costCenterCode = "1100"
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

            val result = service.getMasters(null, null, null, null, true, PageRequest.of(0, 20))

            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
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

            val bytes = service.exportToExcel(null, null, null, null, true)

            // xlsx 파일 시그니처 (PK\x03\x04 — ZIP 형식) 확인
            assertThat(bytes).isNotEmpty
            assertThat(bytes[0]).isEqualTo(0x50.toByte()) // 'P'
            assertThat(bytes[1]).isEqualTo(0x4B.toByte()) // 'K'
        }

        @Test
        @DisplayName("성공 - 결과 0건 -> 헤더만 있는 빈 xlsx 반환")
        fun exportToExcel_empty() {
            val page = PageImpl<PPTMasterSearchResult>(emptyList(), PageRequest.of(0, 50_000), 0)
            every {
                pptMasterRepository.searchMasters(any(), any(), any(), any(), any(), any(), any())
            } returns page

            val bytes = service.exportToExcel(null, null, null, null, true)

            assertThat(bytes).isNotEmpty
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
        @DisplayName("성공 - 종료일 도래, 다른 유효 마스터 없음 -> 사원 미배정(null)으로 복귀")
        fun expireMasters_revertToDefault() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, endDate = today)
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            every { pptMasterRepository.findExpiringMasters(today) } returns listOf(master)
            every { pptMasterRepository.findValidMastersByEmployeeId(1L, today) } returns listOf(master)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
            stubTeamMemberScheduleDelete()

            batchService.expireMasters()

            assertThat(employee.professionalPromotionTeam).isNull()
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
            every { pptMasterRepository.findValidMastersByEmployeeId(1L, today) } returns listOf(master)
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
        @DisplayName("성공 - 필터 없이 호출 → repository searchHistories 호출 + 응답 사원 컨텍스트 정상")
        fun getAllHistory_noFilter_success() {
            val employee = createEmployee()
            val history = ProfessionalPromotionTeamHistory(
                id = 1L,
                name = "PH0000001",
                employeeId = employee.id,
                oldValue = ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                employee = employee
            )
            val page = PageImpl(listOf(history), PageRequest.of(0, 20), 1)

            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.getAllHistory(null, null, null, null, null, PageRequest.of(0, 20))

            assertThat(result.content).hasSize(1)
            val row = result.content[0]
            assertThat(row.name).isEqualTo("PH0000001")
            assertThat(row.employeeId).isEqualTo(employee.id)
            assertThat(row.employeeName).isEqualTo("홍길동")
            assertThat(row.employeeCode).isEqualTo("12345678")
            assertThat(row.orgName).isEqualTo("서울지점")
            assertThat(row.oldValue).isEqualTo(ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING)
            assertThat(row.newValue).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }

        @Test
        @DisplayName("성공 - teamType 표시명 → enum 변환하여 repository 호출")
        fun getAllHistory_teamTypeDisplayName_converted() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getAllHistory(null, null, "라면세일조", null, null, PageRequest.of(0, 20))

            verify {
                pptHistoryRepository.searchHistories(
                    any(), any(),
                    ProfessionalPromotionTeamType.RAMEN_SALE,
                    any(), any(), any()
                )
            }
        }

        @Test
        @DisplayName("성공 - 잘못된 teamType 문자열 → null 변환 (예외 없음)")
        fun getAllHistory_invalidTeamType_nullConverted() {
            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getAllHistory(null, null, "잘못된값", null, null, PageRequest.of(0, 20))

            verify {
                pptHistoryRepository.searchHistories(
                    any(), any(), null,
                    any(), any(), any()
                )
            }
        }

        @Test
        @DisplayName("성공 - 사원 lookup 결과가 null 인 row → 사원 컨텍스트 3 필드 null")
        fun getAllHistory_deletedEmployee_nullContext() {
            val history = ProfessionalPromotionTeamHistory(
                id = 99L,
                employeeId = 999L,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                employee = null
            )
            val page = PageImpl(listOf(history), PageRequest.of(0, 20), 1)

            every {
                pptHistoryRepository.searchHistories(any(), any(), any(), any(), any(), any())
            } returns page

            val result = service.getAllHistory(null, null, null, null, null, PageRequest.of(0, 20))

            assertThat(result.content[0].employeeName).isNull()
            assertThat(result.content[0].employeeCode).isNull()
            assertThat(result.content[0].orgName).isNull()
        }
    }

    @Nested
    @DisplayName("getHistory - 사원 컨텍스트 3 필드 보강")
    inner class GetHistoryEnrichmentTests {

        @Test
        @DisplayName("성공 - getHistory 응답에 name/employeeCode/orgName 포함")
        fun getHistory_includesEmployeeContext() {
            val master = createMaster(employeeId = 1L)
            val employee = createEmployee()
            val history = ProfessionalPromotionTeamHistory(
                id = 1L,
                name = "PH0000001",
                employeeId = 1L,
                oldValue = null,
                newValue = ProfessionalPromotionTeamType.RAMEN_SALE
            )

            every { pptMasterRepository.findById(1L) } returns Optional.of(master)
            every {
                pptHistoryRepository.findByEmployeeIdOrderByChangedAtDesc(1L, any())
            } returns PageImpl(listOf(history), PageRequest.of(0, 20), 1)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)

            val result = service.getHistory(1L, PageRequest.of(0, 20))

            assertThat(result.content[0].name).isEqualTo("PH0000001")
            assertThat(result.content[0].employeeName).isEqualTo("홍길동")
            assertThat(result.content[0].employeeCode).isEqualTo("12345678")
            assertThat(result.content[0].orgName).isEqualTo("서울지점")
        }
    }
}
