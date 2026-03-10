package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.InvalidWorkStatusException
import com.otoki.internal.promotion.exception.InvalidWorkType3Exception
import com.otoki.internal.promotion.exception.PromotionEmployeeNotFoundException
import com.otoki.internal.promotion.exception.PromotionNotFoundException
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
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
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPromotionEmployeeService 테스트")
class AdminPromotionEmployeeServiceTest {

    @Mock private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository
    @Mock private lateinit var promotionRepository: PromotionRepository
    @Mock private lateinit var userRepository: UserRepository

    @InjectMocks private lateinit var service: AdminPromotionEmployeeService

    @Nested
    @DisplayName("getEmployees - 행사조원 목록 조회")
    inner class GetEmployeesTests {

        @Test
        @DisplayName("정상 조회 - 행사에 조원 존재 -> 목록 반환")
        fun getEmployees_success() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))

            val employees = listOf(
                createPromotionEmployee(id = 1L, scheduleDate = LocalDate.of(2026, 3, 15)),
                createPromotionEmployee(id = 2L, scheduleDate = LocalDate.of(2026, 3, 16))
            )
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(employees)

            val user = createUser()
            whenever(userRepository.findBySfidIn(listOf("a0B5g00000XYZabc"))).thenReturn(listOf(user))

            val result = service.getEmployees(10L)

            assertThat(result).hasSize(2)
            assertThat(result[0].employeeName).isEqualTo("김여사")
            assertThat(result[0].scheduleDate).isEqualTo(LocalDate.of(2026, 3, 15))
        }

        @Test
        @DisplayName("빈 목록 - 조원 없음 -> 빈 배열 반환")
        fun getEmployees_empty() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(emptyList())

            val result = service.getEmployees(10L)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("행사 미존재 -> PromotionNotFoundException")
        fun getEmployees_promotionNotFound() {
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.getEmployees(999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사 -> PromotionNotFoundException")
        fun getEmployees_deletedPromotion() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { service.getEmployees(10L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getEmployee - 행사조원 상세 조회")
    inner class GetEmployeeTests {

        @Test
        @DisplayName("정상 조회 -> 상세 반환")
        fun getEmployee_success() {
            val pe = createPromotionEmployee()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            val user = createUser()
            whenever(userRepository.findBySfidIn(listOf("a0B5g00000XYZabc"))).thenReturn(listOf(user))

            val result = service.getEmployee(1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.employeeName).isEqualTo("김여사")
            assertThat(result.createdAt).isNotNull()
        }

        @Test
        @DisplayName("미존재 -> PromotionEmployeeNotFoundException")
        fun getEmployee_notFound() {
            whenever(promotionEmployeeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.getEmployee(999L) }
                .isInstanceOf(PromotionEmployeeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("createEmployee - 행사조원 등록")
    inner class CreateEmployeeTests {

        @Test
        @DisplayName("정상 등록 -> 생성된 조원 반환")
        fun createEmployee_success() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))

            val request = createRequest()
            val result = service.createEmployee(10L, request)

            assertThat(result.employeeSfid).isEqualTo("a0B5g00000XYZabc")
            assertThat(result.workStatus).isEqualTo("근무")
            assertThat(result.scheduleId).isNull()
            assertThat(result.promoCloseByTm).isFalse()
        }

        @Test
        @DisplayName("선택적 필드 null -> 성공")
        fun createEmployee_optionalFieldsNull() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(emptyList())

            val request = createRequest(workType4 = null, professionalPromotionTeam = null, basePrice = null, dailyTargetCount = null)
            val result = service.createEmployee(10L, request)

            assertThat(result.workType4).isNull()
            assertThat(result.professionalPromotionTeam).isNull()
            assertThat(result.basePrice).isNull()
            assertThat(result.dailyTargetCount).isNull()
        }

        @Test
        @DisplayName("행사 미존재 -> PromotionNotFoundException")
        fun createEmployee_promotionNotFound() {
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.createEmployee(999L, createRequest()) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사 -> PromotionNotFoundException")
        fun createEmployee_deletedPromotion() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { service.createEmployee(10L, createRequest()) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("잘못된 work_status -> InvalidWorkStatusException")
        fun createEmployee_invalidWorkStatus() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { service.createEmployee(10L, createRequest(workStatus = "출장")) }
                .isInstanceOf(InvalidWorkStatusException::class.java)
        }

        @Test
        @DisplayName("잘못된 work_type3 -> InvalidWorkType3Exception")
        fun createEmployee_invalidWorkType3() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { service.createEmployee(10L, createRequest(workType3 = "파견")) }
                .isInstanceOf(InvalidWorkType3Exception::class.java)
        }
    }

    @Nested
    @DisplayName("updateEmployee - 행사조원 수정")
    inner class UpdateEmployeeTests {

        @Test
        @DisplayName("정상 수정 -> 수정된 조원 반환")
        fun updateEmployee_success() {
            val pe = createPromotionEmployee()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser()))

            val request = createRequest(scheduleDate = LocalDate.of(2026, 3, 20))
            val result = service.updateEmployee(1L, request)

            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 20))
        }

        @Test
        @DisplayName("미존재 -> PromotionEmployeeNotFoundException")
        fun updateEmployee_notFound() {
            whenever(promotionEmployeeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.updateEmployee(999L, createRequest()) }
                .isInstanceOf(PromotionEmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("잘못된 work_status -> InvalidWorkStatusException")
        fun updateEmployee_invalidWorkStatus() {
            val pe = createPromotionEmployee()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            assertThatThrownBy { service.updateEmployee(1L, createRequest(workStatus = "출장")) }
                .isInstanceOf(InvalidWorkStatusException::class.java)
        }

        @Test
        @DisplayName("잘못된 work_type3 -> InvalidWorkType3Exception")
        fun updateEmployee_invalidWorkType3() {
            val pe = createPromotionEmployee()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            assertThatThrownBy { service.updateEmployee(1L, createRequest(workType3 = "파견")) }
                .isInstanceOf(InvalidWorkType3Exception::class.java)
        }
    }

    @Nested
    @DisplayName("deleteEmployee - 행사조원 삭제")
    inner class DeleteEmployeeTests {

        @Test
        @DisplayName("정상 삭제 -> hard delete")
        fun deleteEmployee_success() {
            val pe = createPromotionEmployee()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            service.deleteEmployee(1L)

            verify(promotionEmployeeRepository).delete(pe)
        }

        @Test
        @DisplayName("미존재 -> PromotionEmployeeNotFoundException")
        fun deleteEmployee_notFound() {
            whenever(promotionEmployeeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.deleteEmployee(999L) }
                .isInstanceOf(PromotionEmployeeNotFoundException::class.java)
        }
    }

    // --- Helper methods ---

    private fun createPromotion(
        id: Long = 10L,
        isDeleted: Boolean = false
    ): Promotion = Promotion(
        id = id,
        promotionNumber = "PM00000001",
        promotionName = "GS25 역삼점 3월 라면 행사",
        accountId = 100,
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        isDeleted = isDeleted
    )

    private fun createPromotionEmployee(
        id: Long = 1L,
        promotionId: Long = 10L,
        employeeSfid: String = "a0B5g00000XYZabc",
        scheduleDate: LocalDate = LocalDate.of(2026, 3, 15)
    ): PromotionEmployee = PromotionEmployee(
        id = id,
        promotionId = promotionId,
        employeeSfid = employeeSfid,
        scheduleDate = scheduleDate,
        workStatus = "근무",
        workType1 = "시식",
        workType3 = "고정",
        workType4 = "냉장",
        professionalPromotionTeam = "라면세일조",
        basePrice = 1500,
        dailyTargetCount = 100
    )

    private fun createUser(
        sfid: String = "a0B5g00000XYZabc",
        name: String = "김여사"
    ): User = User(
        id = 1L,
        sfid = sfid,
        employeeId = "20030117",
        name = name
    )

    private fun createRequest(
        employeeSfid: String = "a0B5g00000XYZabc",
        scheduleDate: LocalDate = LocalDate.of(2026, 3, 15),
        workStatus: String = "근무",
        workType1: String = "시식",
        workType3: String = "고정",
        workType4: String? = "냉장",
        professionalPromotionTeam: String? = "라면세일조",
        basePrice: Long? = 1500,
        dailyTargetCount: Int? = 100
    ): PromotionEmployeeRequest = PromotionEmployeeRequest(
        employeeSfid = employeeSfid,
        scheduleDate = scheduleDate,
        workStatus = workStatus,
        workType1 = workType1,
        workType3 = workType3,
        workType4 = workType4,
        professionalPromotionTeam = professionalPromotionTeam,
        basePrice = basePrice,
        dailyTargetCount = dailyTargetCount
    )
}
