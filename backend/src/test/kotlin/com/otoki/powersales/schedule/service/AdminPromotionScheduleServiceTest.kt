package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkDeleteRequest
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkUpdateItem
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkUpdateRequest
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.PromotionScheduleBulkDeleteInvalidSizeException
import com.otoki.powersales.schedule.exception.PromotionScheduleBulkDuplicateException
import com.otoki.powersales.schedule.exception.PromotionScheduleBulkInvalidSizeException
import com.otoki.powersales.schedule.exception.PromotionScheduleInvalidWorkingCategoryException
import com.otoki.powersales.schedule.exception.PromotionScheduleNotFoundPartialException
import com.otoki.powersales.schedule.exception.PromotionScheduleNotInPromotionException
import com.otoki.powersales.schedule.exception.PromotionScheduleWorkingDateOutOfPromotionException
import com.otoki.powersales.schedule.exception.TeamScheduleAccountNotFoundException
import com.otoki.powersales.schedule.exception.TeamScheduleConflictException
import com.otoki.powersales.schedule.exception.TeamScheduleNotFoundException
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify

@DisplayName("AdminPromotionScheduleService 테스트")
class AdminPromotionScheduleServiceTest {

    private val promotionRepository: PromotionRepository = mockk(relaxUnitFun = true)
    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk(relaxUnitFun = true)
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleCascadeHelper: com.otoki.powersales.schedule.service.TeamMemberScheduleCascadeHelper =
        mockk(relaxUnitFun = true)
    private val principal: com.otoki.powersales.auth.web.WebUserPrincipal = mockk(relaxed = true)
    private val promotionSchedulesUpsertHelper: com.otoki.powersales.promotion.service.PromotionSchedulesUpsertHelper = mockk(relaxUnitFun = true)

    private lateinit var service: AdminPromotionScheduleService

    private val promotionId = 100L
    private val startDate = LocalDate.of(2026, 5, 1)
    private val endDate = LocalDate.of(2026, 5, 7)

    @BeforeEach
    fun setUpService() {
        val teamScheduleValidator = TeamScheduleValidator(
            teamMemberScheduleRepository,
            displayWorkScheduleRepository
        )
        service = AdminPromotionScheduleService(
            promotionRepository = promotionRepository,
            promotionEmployeeRepository = promotionEmployeeRepository,
            teamMemberScheduleRepository = teamMemberScheduleRepository,
            accountRepository = accountRepository,
            teamScheduleValidator = teamScheduleValidator,
            teamMemberScheduleCascadeHelper = teamMemberScheduleCascadeHelper,
            promotionSchedulesUpsertHelper = promotionSchedulesUpsertHelper
        )

        // Spec #693 Q3 — promoCloseByTm 가드 기본 false (테스트 별 override)
        every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(any()) } returns false
    }

    // ========== getSchedules ==========

    @Nested
    @DisplayName("getSchedules - 행사 배치원 일정 목록")
    inner class GetSchedulesTests {

        @Test
        @DisplayName("정상 조회 - 행사 배치원 + 일정 트리 반환")
        fun getSchedules_success() {
            // Given
            val promotion = createPromotion()
            val employee = createEmployee(id = 50L)
            val account = createAccount(id = 300, name = "이마트 강남점", externalKey = "SAP001")
            val pe = createPromotionEmployee(id = 200L, employee = employee)
            val schedule = createSchedule(
                id = 1001L,
                employee = employee,
                account = account,
                workingDate = LocalDate.of(2026, 5, 1),
                workingCategory1 = WorkingCategory1.EVENT,
                workingCategory3 = WorkingCategory3.FIXED,
                promotionEmployee = pe
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId) } returns listOf(pe)
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(eq(listOf(50L)), eq(startDate), eq(endDate), any()) } returns listOf(schedule)

            // When
            val result = service.getSchedules(promotionId, null, null)

            // Then
            assertThat(result.promotionId).isEqualTo(promotionId)
            assertThat(result.schedulePeriod.startDate).isEqualTo(startDate)
            assertThat(result.totalMemberCount).isEqualTo(1)
            assertThat(result.totalScheduleCount).isEqualTo(1)
            assertThat(result.members).hasSize(1)
            val member = result.members[0]
            assertThat(member.employeeName).isEqualTo("홍길동")
            assertThat(member.schedules[0].scheduleId).isEqualTo(1001L)
            assertThat(member.schedules[0].accountCode).isEqualTo("SAP001")
            assertThat(member.schedules[0].accountName).isEqualTo("이마트 강남점")
        }

        @Test
        @DisplayName("미존재 행사 - PromotionNotFoundException")
        fun getSchedules_promotionNotFound() {
            every { promotionRepository.findById(promotionId) } returns Optional.empty()

            assertThatThrownBy { service.getSchedules(promotionId, null, null) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("동일 사원 다일자 배치 — PE N건 × TMS N건이 N×N 으로 부풀려지지 않아야 함")
        fun getSchedules_sameEmployeeMultipleDays_noCrossProduct() {
            // Given — 송은주 1명이 같은 행사에 5/2~5/5 4일 배치 (PE 4건, TMS 4건, 1:1 매칭)
            val promotion = createPromotion()
            val employee = createEmployee(id = 50L)
            val account = createAccount(id = 300)
            val dates = listOf(
                LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5)
            )
            val promotionEmployees = dates.mapIndexed { i, _ ->
                createPromotionEmployee(id = 200L + i, employee = employee, promotionId = promotionId)
            }
            val schedules = promotionEmployees.mapIndexed { i, pe ->
                createSchedule(
                    id = 1000L + i, employee = employee, account = account,
                    workingDate = dates[i], promotionEmployee = pe
                )
            }

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId) } returns promotionEmployees
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(any(), any(), any(), any()) } returns schedules

            // When
            val result = service.getSchedules(promotionId, null, null)

            // Then — PE 4건 각각에 해당 PE 의 일정 1건만 매칭. 총 4건 (16건 회귀 방지)
            assertThat(result.totalMemberCount).isEqualTo(4)
            assertThat(result.totalScheduleCount).isEqualTo(4)
            result.members.forEach { m -> assertThat(m.schedules).hasSize(1) }
        }

        @Test
        @DisplayName("다른 행사 일정 필터링 - 동일 사원이 다른 행사에도 배정된 경우 본 행사 일정만 반환")
        fun getSchedules_filtersOtherPromotion() {
            val promotion = createPromotion()
            val employee = createEmployee(id = 50L)
            val account = createAccount(id = 300)
            val peThis = createPromotionEmployee(id = 200L, employee = employee, promotionId = promotionId)
            val peOther = createPromotionEmployee(id = 999L, employee = employee, promotionId = 999L)
            val scheduleThis = createSchedule(
                id = 1L, employee = employee, account = account,
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = peThis
            )
            val scheduleOther = createSchedule(
                id = 2L, employee = employee, account = account,
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = peOther
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId) } returns listOf(peThis)
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(any(), any(), any(), any()) } returns listOf(scheduleThis, scheduleOther)

            val result = service.getSchedules(promotionId, null, null)

            assertThat(result.totalScheduleCount).isEqualTo(1)
            assertThat(result.members[0].schedules[0].scheduleId).isEqualTo(1L)
        }
    }

    // ========== bulkUpdate ==========

    @Nested
    @DisplayName("bulkUpdate - 행사 일정 일괄 변경")
    inner class BulkUpdateTests {

        @Test
        @DisplayName("정상 변경 - 2건 일괄 변경 -> updatedCount=2")
        fun bulkUpdate_success() {
            val promotion = createPromotion()
            val emp1 = createEmployee(id = 50L)
            val emp2 = createEmployee(id = 51L, employeeCode = "20030002", name = "김영희")
            val pe1 = createPromotionEmployee(id = 200L, employee = emp1)
            val pe2 = createPromotionEmployee(id = 201L, employee = emp2)
            val oldAccount = createAccount(id = 300)
            val newAccount = createAccount(id = 301, name = "홈플러스 역삼점")
            val schedule1 = createSchedule(
                id = 1001L, employee = emp1, account = oldAccount,
                workingDate = LocalDate.of(2026, 5, 1), workingCategory1 = WorkingCategory1.EVENT, workingCategory3 = WorkingCategory3.FIXED,
                promotionEmployee = pe1
            )
            val schedule2 = createSchedule(
                id = 1002L, employee = emp2, account = oldAccount,
                workingDate = LocalDate.of(2026, 5, 2), workingCategory1 = WorkingCategory1.EVENT, workingCategory3 = WorkingCategory3.FIXED,
                promotionEmployee = pe2
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L, 1002L)) } returns listOf(schedule1, schedule2)
            every { accountRepository.findAllById(listOf(301)) } returns listOf(newAccount)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(any(), any()) } returns emptyList()
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers { firstArg<List<TeamMemberSchedule>>() }

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                PromotionScheduleBulkUpdateItem(
                    scheduleId = 1001L, accountId = 301,
                    workingDate = LocalDate.of(2026, 5, 3),
                    workingCategory1 = "행사", workingCategory3 = "고정", workingCategory4 = null
                ),
                PromotionScheduleBulkUpdateItem(
                    scheduleId = 1002L, accountId = 301,
                    workingDate = LocalDate.of(2026, 5, 4),
                    workingCategory1 = "행사", workingCategory3 = "순회", workingCategory4 = null
                )
            ))

            val result = service.bulkUpdate(promotionId, request)

            assertThat(result.updatedCount).isEqualTo(2)
            assertThat(result.scheduleIds).containsExactly(1001L, 1002L)
            assertThat(schedule1.account?.id).isEqualTo(301)
            assertThat(schedule1.workingDate).isEqualTo(LocalDate.of(2026, 5, 3))
            assertThat(schedule2.workingCategory3?.displayName).isEqualTo("순회")
        }

        @Test
        @DisplayName("미존재 행사 - PromotionNotFoundException")
        fun bulkUpdate_promotionNotFound() {
            every { promotionRepository.findById(promotionId) } returns Optional.empty()

            val request = singleItemRequest()
            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("미존재 schedule_id - TeamScheduleNotFoundException, 전체 롤백")
        fun bulkUpdate_scheduleNotFound() {
            every { promotionRepository.findById(promotionId) } returns Optional.of(createPromotion())
            every { teamMemberScheduleRepository.findAllById(listOf(9999L)) } returns emptyList()

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                bulkItem(scheduleId = 9999L)
            ))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(TeamScheduleNotFoundException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("다른 행사 소속 - PromotionScheduleNotInPromotionException")
        fun bulkUpdate_differentPromotion() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val peOther = createPromotionEmployee(id = 999L, employee = emp, promotionId = 999L)
            val schedule = createSchedule(
                id = 1001L, employee = emp, account = createAccount(id = 300),
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = peOther
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L)) } returns listOf(schedule)

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(bulkItem(scheduleId = 1001L)))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(PromotionScheduleNotInPromotionException::class.java)
        }

        @Test
        @DisplayName("미존재 account_id - TeamScheduleAccountNotFoundException")
        fun bulkUpdate_accountNotFound() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val pe = createPromotionEmployee(id = 200L, employee = emp)
            val schedule = createSchedule(
                id = 1001L, employee = emp, account = createAccount(id = 300),
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = pe
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L)) } returns listOf(schedule)
            every { accountRepository.findAllById(listOf(9999)) } returns emptyList()

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                bulkItem(scheduleId = 1001L, accountId = 9999)
            ))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(TeamScheduleAccountNotFoundException::class.java)
        }

        @Test
        @DisplayName("행사기간 밖 - PromotionScheduleWorkingDateOutOfPromotionException")
        fun bulkUpdate_workingDateOutOfRange() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val pe = createPromotionEmployee(id = 200L, employee = emp)
            val schedule = createSchedule(
                id = 1001L, employee = emp, account = createAccount(id = 300),
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = pe
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L)) } returns listOf(schedule)

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                bulkItem(scheduleId = 1001L, workingDate = LocalDate.of(2026, 6, 15))
            ))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(PromotionScheduleWorkingDateOutOfPromotionException::class.java)
        }

        @Test
        @DisplayName("invalid working_category1 - PromotionScheduleInvalidWorkingCategoryException")
        fun bulkUpdate_invalidCategory() {
            every { promotionRepository.findById(promotionId) } returns Optional.of(createPromotion())

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                bulkItem(scheduleId = 1001L, workingCategory1 = "외근")
            ))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(PromotionScheduleInvalidWorkingCategoryException::class.java)
        }

        @Test
        @DisplayName("요청 내 (employeeId, workingDate) 중복 - PromotionScheduleBulkDuplicateException")
        fun bulkUpdate_duplicateInRequest() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val pe = createPromotionEmployee(id = 200L, employee = emp)
            val account = createAccount(id = 300)
            val schedule1 = createSchedule(
                id = 1001L, employee = emp, account = account,
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = pe
            )
            val schedule2 = createSchedule(
                id = 1002L, employee = emp, account = account,
                workingDate = LocalDate.of(2026, 5, 2), promotionEmployee = pe
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L, 1002L)) } returns listOf(schedule1, schedule2)

            val sameDate = LocalDate.of(2026, 5, 3)
            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                bulkItem(scheduleId = 1001L, workingDate = sameDate),
                bulkItem(scheduleId = 1002L, workingDate = sameDate)
            ))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(PromotionScheduleBulkDuplicateException::class.java)
        }

        @Test
        @DisplayName("DB 일정 충돌 - TeamScheduleConflictException")
        fun bulkUpdate_conflict() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val pe = createPromotionEmployee(id = 200L, employee = emp)
            val account = createAccount(id = 300)
            val schedule = createSchedule(
                id = 1001L, employee = emp, account = account,
                workingDate = LocalDate.of(2026, 5, 1), workingCategory1 = WorkingCategory1.EVENT, workingCategory3 = WorkingCategory3.FIXED,
                promotionEmployee = pe
            )
            // 변경 후 working_date 에 동일 사원의 다른 고정 일정이 이미 존재
            val conflicting = createSchedule(
                id = 2002L, employee = emp, account = createAccount(id = 301),
                workingDate = LocalDate.of(2026, 5, 5), workingCategory1 = WorkingCategory1.EVENT, workingCategory3 = WorkingCategory3.FIXED,
                promotionEmployee = createPromotionEmployee(id = 201L, employee = emp)
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L)) } returns listOf(schedule)
            every { accountRepository.findAllById(listOf(300)) } returns listOf(account)
            every { teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(50L, LocalDate.of(2026, 5, 5)) } returns listOf(conflicting)

            val request = PromotionScheduleBulkUpdateRequest(items = listOf(
                bulkItem(scheduleId = 1001L, accountId = 300, workingDate = LocalDate.of(2026, 5, 5))
            ))

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("500건 초과 - PromotionScheduleBulkInvalidSizeException")
        fun bulkUpdate_tooManyItems() {
            every { promotionRepository.findById(promotionId) } returns Optional.of(createPromotion())

            val items = (1L..501L).map { bulkItem(scheduleId = it, workingDate = LocalDate.of(2026, 5, 1)) }
            val request = PromotionScheduleBulkUpdateRequest(items = items)

            assertThatThrownBy { service.bulkUpdate(promotionId, request) }
                .isInstanceOf(PromotionScheduleBulkInvalidSizeException::class.java)
        }
    }

    // ========== bulkDelete ==========

    @Nested
    @DisplayName("bulkDelete - 행사 일정 일괄 삭제")
    inner class BulkDeleteTests {

        @Test
        @DisplayName("정상 삭제 - 3건 -> deletedCount=3")
        fun bulkDelete_success() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val pe = createPromotionEmployee(id = 200L, employee = emp)
            val account = createAccount(id = 300)
            val schedules = listOf(
                createSchedule(id = 1001L, employee = emp, account = account, workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = pe),
                createSchedule(id = 1002L, employee = emp, account = account, workingDate = LocalDate.of(2026, 5, 2), promotionEmployee = pe),
                createSchedule(id = 1003L, employee = emp, account = account, workingDate = LocalDate.of(2026, 5, 3), promotionEmployee = pe)
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L, 1002L, 1003L)) } returns schedules

            val result = service.bulkDelete(principal, promotionId, PromotionScheduleBulkDeleteRequest(listOf(1001L, 1002L, 1003L)))

            assertThat(result.deletedCount).isEqualTo(3)
            verify { teamMemberScheduleCascadeHelper.cascadeDeleteSchedules(principal, schedules) }
        }

        @Test
        @DisplayName("부분 미존재 - PromotionScheduleNotFoundPartialException, missing_ids 포함")
        fun bulkDelete_partialNotFound() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val pe = createPromotionEmployee(id = 200L, employee = emp)
            val schedule1 = createSchedule(id = 1001L, employee = emp, account = createAccount(id = 300),
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = pe)

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L, 9999L)) } returns listOf(schedule1)

            assertThatThrownBy {
                service.bulkDelete(principal, promotionId, PromotionScheduleBulkDeleteRequest(listOf(1001L, 9999L)))
            }.isInstanceOf(PromotionScheduleNotFoundPartialException::class.java)
                .satisfies({ ex ->
                    val partial = ex as PromotionScheduleNotFoundPartialException
                    assertThat(partial.missingIds).containsExactly(9999L)
                })

            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("다른 행사 소속 - PromotionScheduleNotInPromotionException, 전체 롤백")
        fun bulkDelete_differentPromotion() {
            val promotion = createPromotion()
            val emp = createEmployee(id = 50L)
            val peOther = createPromotionEmployee(id = 999L, employee = emp, promotionId = 999L)
            val schedule = createSchedule(
                id = 1001L, employee = emp, account = createAccount(id = 300),
                workingDate = LocalDate.of(2026, 5, 1), promotionEmployee = peOther
            )

            every { promotionRepository.findById(promotionId) } returns Optional.of(promotion)
            every { teamMemberScheduleRepository.findAllById(listOf(1001L)) } returns listOf(schedule)

            assertThatThrownBy {
                service.bulkDelete(principal, promotionId, PromotionScheduleBulkDeleteRequest(listOf(1001L)))
            }.isInstanceOf(PromotionScheduleNotInPromotionException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("500건 초과 - PromotionScheduleBulkDeleteInvalidSizeException")
        fun bulkDelete_tooMany() {
            every { promotionRepository.findById(promotionId) } returns Optional.of(createPromotion())

            val ids = (1L..501L).toList()

            assertThatThrownBy {
                service.bulkDelete(principal, promotionId, PromotionScheduleBulkDeleteRequest(ids))
            }.isInstanceOf(PromotionScheduleBulkDeleteInvalidSizeException::class.java)
        }
    }

    // --- Helper factories ---

    private fun bulkItem(
        scheduleId: Long = 1001L,
        accountId: Int = 300,
        workingDate: LocalDate = LocalDate.of(2026, 5, 1),
        workingCategory1: String? = "행사",
        workingCategory3: String? = "고정",
        workingCategory4: String? = null
    ) = PromotionScheduleBulkUpdateItem(
        scheduleId = scheduleId,
        accountId = accountId,
        workingDate = workingDate,
        workingCategory1 = workingCategory1,
        workingCategory3 = workingCategory3,
        workingCategory4 = workingCategory4
    )

    private fun singleItemRequest() = PromotionScheduleBulkUpdateRequest(items = listOf(bulkItem()))

    private fun createPromotion(
        id: Long = promotionId,
        startDate: LocalDate = this.startDate,
        endDate: LocalDate = this.endDate
    ): Promotion = Promotion(
        id = id,
        promotionNumber = "P${id}",
        startDate = startDate,
        endDate = endDate,
        account = createAccount(id = 1)
    )

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "20030001",
        name: String = "홍길동"
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name
    )

    private fun createAccount(
        id: Int = 1,
        name: String = "테스트거래처",
        externalKey: String? = "EXT$id"
    ): Account = Account(id = id, name = name, externalKey = externalKey)

    private fun createPromotionEmployee(
        id: Long = 200L,
        promotionId: Long = this.promotionId,
        employee: Employee = createEmployee()
    ): PromotionEmployee {
        val pe = PromotionEmployee(
            id = id,
            promotionId = promotionId,
            employeeId = employee.id
        )
        pe.employee = employee
        return pe
    }

    private fun createSchedule(
        id: Long = 1001L,
        employee: Employee = createEmployee(),
        account: Account? = createAccount(),
        workingDate: LocalDate? = LocalDate.of(2026, 5, 1),
        workingType: WorkingType? = WorkingType.WORK,
        workingCategory1: WorkingCategory1? = WorkingCategory1.EVENT,
        workingCategory3: WorkingCategory3? = WorkingCategory3.FIXED,
        promotionEmployee: PromotionEmployee? = null
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = employee,
        account = account,
        workingDate = workingDate,
        workingType = workingType,
        workingCategory1 = workingCategory1,
        workingCategory3 = workingCategory3,
        promotionEmployee = promotionEmployee
    )
}
