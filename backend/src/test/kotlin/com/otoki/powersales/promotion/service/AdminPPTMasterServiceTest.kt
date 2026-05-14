package com.otoki.powersales.promotion.service

import com.otoki.powersales.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PPTHistoryRepository
import com.otoki.powersales.promotion.repository.PPTMasterRepository
import com.otoki.powersales.promotion.repository.PPTMasterSearchResult
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.promotion.service.AdminPPTMasterService
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPPTMasterService 테스트")
class AdminPPTMasterServiceTest {

    @Mock private lateinit var pptMasterRepository: PPTMasterRepository
    @Mock private lateinit var pptHistoryRepository: PPTHistoryRepository
    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    @InjectMocks private lateinit var service: AdminPPTMasterService

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
        emp.role = UserRole.WOMAN
        emp.professionalPromotionTeam = professionalPromotionTeam
        return emp
    }

    private fun createAccount(
        id: Int = 1,
        externalKey: String? = "SAP001",
        name: String? = "이마트 강남점"
    ): Account {
        return Account(id = id, externalKey = externalKey, name = name)
    }

    private fun createMaster(
        id: Long = 1L,
        employeeId: Long = 1L,
        accountId: Int = 1,
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

    @Nested
    @DisplayName("getMaster - 마스터 상세 조회")
    inner class GetMasterTests {

        @Test
        @DisplayName("성공 - 존재하는 ID -> 마스터 상세 반환")
        fun getMaster_success() {
            val master = createMaster()
            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))

            val result = service.getMaster(1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.teamType).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            assertThat(result.employeeName).isEqualTo("홍길동")
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> PPTMasterNotFoundException")
        fun getMaster_notFound() {
            whenever(pptMasterRepository.findById(999L)).thenReturn(Optional.empty())

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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(emptyList())
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }

            val result = service.createMaster(request)

            assertThat(result.teamType).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify(pptHistoryRepository, never()).save(any())
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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(emptyList())
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            whenever(pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamHistory>(0) }

            service.createMaster(request)

            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify(pptHistoryRepository).save(any())
        }

        @Test
        @DisplayName("성공 - 기존 유효 마스터 자동 종료")
        fun createMaster_autoTerminate() {
            val existingMaster = createMaster(id = 10L, teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)
            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = false
            )
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(listOf(existingMaster))
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }

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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))

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
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(999)).thenReturn(Optional.empty())

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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(listOf(createMaster()))

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
            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }

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
            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(2)).thenReturn(Optional.of(newAccount))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }

            val result = service.updateMaster(1L, request)

            assertThat(master.accountId).isEqualTo(2)
            assertThat(result.accountCode).isEqualTo("SAP002")
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
            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findById(2)).thenReturn(Optional.of(newAccount))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(listOf(createMaster(id = 2L, accountId = 2)))

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

            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(pptMasterRepository.findValidMastersByEmployeeId(1L, LocalDate.now())).thenReturn(emptyList())
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }

            service.deleteMaster(1L)

            verify(pptMasterRepository).delete(master)
            assertThat(employee.professionalPromotionTeam).isNull()
        }

        @Test
        @DisplayName("성공 - 다른 유효 마스터 존재 -> 사원 변경 없음")
        fun deleteMaster_otherMastersExist() {
            val master = createMaster(id = 1L)
            val otherMaster = createMaster(id = 2L, teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)

            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(pptMasterRepository.findValidMastersByEmployeeId(1L, LocalDate.now()))
                .thenReturn(listOf(otherMaster))

            service.deleteMaster(1L)

            verify(pptMasterRepository).delete(master)
            verify(employeeRepository, never()).save(any())
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> PPTMasterNotFoundException")
        fun deleteMaster_notFound() {
            whenever(pptMasterRepository.findById(999L)).thenReturn(Optional.empty())

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
            val searchResult = PPTMasterSearchResult(master, "12345678", "홍길동", "SAP001", "이마트 강남점")
            val page = PageImpl(listOf(searchResult), PageRequest.of(0, 20), 1)

            whenever(pptMasterRepository.searchMasters(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any(), any()))
                .thenReturn(page)

            val result = service.getMasters(null, null, null, null, true, PageRequest.of(0, 20))

            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
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

            whenever(pptMasterRepository.findValidMasters(LocalDate.now())).thenReturn(listOf(master))
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(employee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            whenever(pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamHistory>(0) }

            batchService.syncValidMasters()

            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            verify(pptHistoryRepository).save(any())
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

            whenever(pptMasterRepository.findExpiringMasters(today)).thenReturn(listOf(master))
            whenever(pptMasterRepository.findValidMastersByEmployeeId(1L, today)).thenReturn(listOf(master))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }

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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(emptyList())
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            whenever(pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamHistory>(0) }

            service.createMaster(request)

            verify(teamMemberScheduleRepository).deleteFutureWorkSchedulesByEmployeeId(eq(1L), eq(today))
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
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(createAccount()))
            whenever(pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(emptyList())
            whenever(pptMasterRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(emptyList())
            whenever(pptMasterRepository.save(any<ProfessionalPromotionTeamMaster>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamMaster>(0) }
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            whenever(pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamHistory>(0) }

            service.createMaster(request)

            verify(teamMemberScheduleRepository, never()).deleteFutureWorkSchedulesByEmployeeId(any(), any())
        }

        @Test
        @DisplayName("성공 - 마스터 삭제 시 일반 복귀 → 미래 근무일정 삭제 호출됨")
        fun deleteMaster_revertToDefault_deletesFutureSchedules() {
            val today = LocalDate.now()
            val master = createMaster()
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            whenever(pptMasterRepository.findById(1L)).thenReturn(Optional.of(master))
            whenever(pptMasterRepository.findValidMastersByEmployeeId(1L, today)).thenReturn(emptyList())
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }

            service.deleteMaster(1L)

            verify(teamMemberScheduleRepository).deleteFutureWorkSchedulesByEmployeeId(eq(1L), eq(today))
        }

        @Test
        @DisplayName("성공 - 배치 동기화 시 PPT 변경 → 미래 근무일정 삭제 호출됨")
        fun syncValidMasters_deletesFutureSchedules() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, teamType = ProfessionalPromotionTeamType.RAMEN_SALE)
            val employee = createEmployee(professionalPromotionTeam = null)

            whenever(pptMasterRepository.findValidMasters(today)).thenReturn(listOf(master))
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(employee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            whenever(pptHistoryRepository.save(any<ProfessionalPromotionTeamHistory>()))
                .thenAnswer { it.getArgument<ProfessionalPromotionTeamHistory>(0) }

            batchService.syncValidMasters()

            verify(teamMemberScheduleRepository).deleteFutureWorkSchedulesByEmployeeId(eq(1L), eq(today))
        }

        @Test
        @DisplayName("성공 - 배치 만료 시 일반 복귀 → 미래 근무일정 삭제 호출됨")
        fun expireMasters_deletesFutureSchedules() {
            val today = LocalDate.now()
            val master = createMaster(employeeId = 1L, endDate = today)
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)

            whenever(pptMasterRepository.findExpiringMasters(today)).thenReturn(listOf(master))
            whenever(pptMasterRepository.findValidMastersByEmployeeId(1L, today)).thenReturn(listOf(master))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }

            batchService.expireMasters()

            verify(teamMemberScheduleRepository).deleteFutureWorkSchedulesByEmployeeId(eq(1L), eq(today))
        }
    }
}
