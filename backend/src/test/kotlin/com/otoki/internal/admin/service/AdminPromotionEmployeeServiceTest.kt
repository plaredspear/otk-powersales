package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.repository.ScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPromotionEmployeeService 테스트")
class AdminPromotionEmployeeServiceTest {

    @Mock private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository
    @Mock private lateinit var promotionRepository: PromotionRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var scheduleRepository: ScheduleRepository

    @InjectMocks private lateinit var service: AdminPromotionEmployeeService

    @Nested
    @DisplayName("getEmployees - 행사조원 목록 조회")
    inner class GetEmployeesTests {

        @Test
        @DisplayName("정상 조회 - 행사에 조원 존재 -> 목록 반환")
        fun getEmployees_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            val employees = listOf(
                createPe(id = 1L, scheduleDate = LocalDate.of(2026, 3, 15)),
                createPe(id = 2L, scheduleDate = LocalDate.of(2026, 3, 16))
            )
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L)).thenReturn(employees)
            whenever(userRepository.findBySfidIn(listOf("a0B5g00000XYZabc"))).thenReturn(listOf(createUser()))

            val result = service.getEmployees(10L)
            assertThat(result).hasSize(2)
            assertThat(result[0].employeeName).isEqualTo("김여사")
        }

        @Test
        @DisplayName("행사 미존재 -> PromotionNotFoundException")
        fun getEmployees_promotionNotFound() {
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())
            assertThatThrownBy { service.getEmployees(999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("createEmployee - 등록 + 전문행사조 매칭")
    inner class CreateEmployeeTests {

        @Test
        @DisplayName("정상 등록 - 라면행사 + 라면세일조 -> 성공")
        fun createEmployee_teamMatch_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "라면")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(professionalPromotionTeam = "라면세일조"))
            assertThat(result.employeeSfid).isEqualTo("a0B5g00000XYZabc")
        }

        @Test
        @DisplayName("전문행사조 null -> 모든 카테고리 허용")
        fun createEmployee_teamNull_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "라면")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(emptyList())
            stubRollup()

            val result = service.createEmployee(10L, createRequest(professionalPromotionTeam = null))
            assertThat(result.professionalPromotionTeam).isNull()
        }

        @Test
        @DisplayName("전문행사조 '일반' -> 모든 카테고리 허용")
        fun createEmployee_teamGeneral_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "냉장")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(emptyList())
            stubRollup()

            service.createEmployee(10L, createRequest(professionalPromotionTeam = "일반"))
        }

        @Test
        @DisplayName("만두행사 + 냉동팀 -> 허용")
        fun createEmployee_manduWithFrozen_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "만두")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(emptyList())
            stubRollup()

            service.createEmployee(10L, createRequest(professionalPromotionTeam = "프레시세일조_냉동"))
        }

        @Test
        @DisplayName("라면행사 + 냉장팀 -> TEAM_CATEGORY_MISMATCH")
        fun createEmployee_teamMismatch() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "라면")))

            assertThatThrownBy { service.createEmployee(10L, createRequest(professionalPromotionTeam = "프레시세일조_냉장")) }
                .isInstanceOf(TeamCategoryMismatchException::class.java)
        }

        @Test
        @DisplayName("만두행사 + 라면팀 -> TEAM_CATEGORY_MISMATCH")
        fun createEmployee_manduRamenMismatch() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "만두")))

            assertThatThrownBy { service.createEmployee(10L, createRequest(professionalPromotionTeam = "라면세일조")) }
                .isInstanceOf(TeamCategoryMismatchException::class.java)
        }
    }

    @Nested
    @DisplayName("updateEmployee - 수정 + 보호 규칙")
    inner class UpdateEmployeeTests {

        @Test
        @DisplayName("정상 수정 -> 성공")
        fun updateEmployee_success() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))
            stubRollup()

            val result = service.updateEmployee(1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 20)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 20))
        }

        @Test
        @DisplayName("마감 조원 비핵심필드 수정 -> 허용")
        fun updateEmployee_closedNonCritical_success() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))
            stubRollup()

            // work_type1만 변경 (비핵심필드)
            val result = service.updateEmployee(1L, createRequest(workType1 = "시음"))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("마감 조원 핵심필드(employee_sfid) 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun updateEmployee_closedCriticalField() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            assertThatThrownBy { service.updateEmployee(1L, createRequest(employeeSfid = "DIFFERENT_SFID")) }
                .isInstanceOf(ClosedEmployeeModificationException::class.java)
        }

        @Test
        @DisplayName("마감 조원 핵심필드(schedule_date) 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun updateEmployee_closedScheduleDate() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            assertThatThrownBy { service.updateEmployee(1L, createRequest(scheduleDate = LocalDate.of(2026, 4, 1))) }
                .isInstanceOf(ClosedEmployeeModificationException::class.java)
        }

        @Test
        @DisplayName("확정 조원 핵심필드 변경 -> 스케줄 삭제 + schedule_id null")
        fun updateEmployee_criticalFieldChange_scheduleDeleted() {
            val pe = createPe(scheduleId = 100L)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))
            stubRollup()

            service.updateEmployee(1L, createRequest(employeeSfid = "NEW_SFID"))

            verify(scheduleRepository).deleteAllByIdIn(listOf(100L))
            assertThat(pe.scheduleId).isNull()
        }

        @Test
        @DisplayName("professionalPromotionTeam 변경 시 -> 스케줄 삭제 안 함")
        fun updateEmployee_teamChange_noScheduleDelete() {
            val pe = createPe(scheduleId = 100L)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))
            stubRollup()

            // team + sfid 동시 변경 -> team 변경이 있으므로 스케줄 삭제 안 함
            service.updateEmployee(1L, createRequest(
                employeeSfid = "NEW_SFID",
                professionalPromotionTeam = "라면세일조B"
            ))

            verify(scheduleRepository, never()).deleteAllByIdIn(any())
            assertThat(pe.scheduleId).isEqualTo(100L)
        }
    }

    @Nested
    @DisplayName("롤업 자동계산 - 행사마스터 금액 갱신")
    inner class RollupTests {

        @Test
        @DisplayName("조원 등록 후 행사마스터 합계 갱신")
        fun createEmployee_updatesPromotionAmounts() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(emptyList())
            stubRollup(targetSum = 100000, actualSum = 80000)

            service.createEmployee(10L, createRequest(targetAmount = 100000, actualAmount = 80000))

            assertThat(promotion.targetAmount).isEqualTo(100000)
            assertThat(promotion.actualAmount).isEqualTo(80000)
        }

        @Test
        @DisplayName("조원 수정 후 행사마스터 합계 갱신")
        fun updateEmployee_updatesPromotionAmounts() {
            val promotion = createPromotion()
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(emptyList())
            stubRollup(targetSum = 150000, actualSum = 110000)

            service.updateEmployee(1L, createRequest(targetAmount = 50000, actualAmount = 30000))

            assertThat(promotion.targetAmount).isEqualTo(150000)
            assertThat(promotion.actualAmount).isEqualTo(110000)
        }

        @Test
        @DisplayName("조원 삭제 후 행사마스터 합계 갱신 (0으로)")
        fun deleteEmployee_updatesPromotionAmounts() {
            val promotion = createPromotion()
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            stubRollup(targetSum = 0, actualSum = 0)

            service.deleteEmployee(1L)

            assertThat(promotion.targetAmount).isEqualTo(0)
            assertThat(promotion.actualAmount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("deleteEmployee - 삭제 + 보호 규칙")
    inner class DeleteEmployeeTests {

        @Test
        @DisplayName("미마감 조원 삭제 -> 성공")
        fun deleteEmployee_success() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            stubRollup()

            service.deleteEmployee(1L)
            verify(promotionEmployeeRepository).delete(pe)
        }

        @Test
        @DisplayName("확정 조원(미마감) 삭제 -> 스케줄 연쇄 삭제")
        fun deleteEmployee_withSchedule_cascadeDelete() {
            val pe = createPe(scheduleId = 100L)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            stubRollup()

            service.deleteEmployee(1L)

            verify(scheduleRepository).deleteAllByIdIn(listOf(100L))
            verify(promotionEmployeeRepository).delete(pe)
        }

        @Test
        @DisplayName("마감 조원 삭제 -> CLOSED_EMPLOYEE_DELETE")
        fun deleteEmployee_closed() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            assertThatThrownBy { service.deleteEmployee(1L) }
                .isInstanceOf(ClosedEmployeeDeleteException::class.java)
        }
    }

    // --- Helpers ---

    private fun stubRollup(promotionId: Long = 10L, targetSum: Long = 0, actualSum: Long = 0) {
        whenever(promotionEmployeeRepository.sumTargetAmountByPromotionId(promotionId)).thenReturn(targetSum)
        whenever(promotionEmployeeRepository.sumActualAmountByPromotionId(promotionId)).thenReturn(actualSum)
        whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
    }

    private fun createPromotion(
        id: Long = 10L,
        isDeleted: Boolean = false,
        category: String? = "라면"
    ) = Promotion(
        id = id, promotionNumber = "PM00000001", promotionName = "테스트 행사",
        accountId = 100, startDate = LocalDate.of(2026, 3, 10), endDate = LocalDate.of(2026, 3, 20),
        isDeleted = isDeleted, category = category
    )

    private fun createPe(
        id: Long = 1L, promotionId: Long = 10L, employeeSfid: String = "a0B5g00000XYZabc",
        scheduleDate: LocalDate = LocalDate.of(2026, 3, 15), scheduleId: Long? = null,
        promoCloseByTm: Boolean = false
    ) = PromotionEmployee(
        id = id, promotionId = promotionId, employeeSfid = employeeSfid, scheduleDate = scheduleDate,
        workStatus = "근무", workType1 = "시식", workType3 = "고정", workType4 = "냉장",
        professionalPromotionTeam = "라면세일조", basePrice = 1500, dailyTargetCount = 100,
        scheduleId = scheduleId, promoCloseByTm = promoCloseByTm
    )

    private fun createUser() = User(id = 1L, sfid = "a0B5g00000XYZabc", employeeId = "20030117", name = "김여사")

    private fun createRequest(
        employeeSfid: String = "a0B5g00000XYZabc", scheduleDate: LocalDate = LocalDate.of(2026, 3, 15),
        workStatus: String = "근무", workType1: String = "시식", workType3: String = "고정",
        workType4: String? = "냉장", professionalPromotionTeam: String? = "라면세일조",
        basePrice: Long? = 1500, dailyTargetCount: Int? = 100,
        targetAmount: Long? = 0, actualAmount: Long? = 0
    ) = PromotionEmployeeRequest(
        employeeSfid = employeeSfid, scheduleDate = scheduleDate, workStatus = workStatus,
        workType1 = workType1, workType3 = workType3, workType4 = workType4,
        professionalPromotionTeam = professionalPromotionTeam, basePrice = basePrice,
        dailyTargetCount = dailyTargetCount, targetAmount = targetAmount, actualAmount = actualAmount
    )
}
