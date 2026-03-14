package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.BatchUpdatePromotionEmployeeItem
import com.otoki.internal.admin.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
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
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

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
            whenever(userRepository.findByEmployeeIdIn(listOf("20030117"))).thenReturn(listOf(createUser()))

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
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(professionalPromotionTeam = "라면세일조"))
            assertThat(result.employeeSfid).isEqualTo("a0B5g00000XYZabc")
            assertThat(result.employeeId).isEqualTo("20030117")
        }

        @Test
        @DisplayName("전문행사조 null -> 모든 카테고리 허용")
        fun createEmployee_teamNull_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "라면")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
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
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            service.createEmployee(10L, createRequest(professionalPromotionTeam = "일반"))
        }

        @Test
        @DisplayName("만두행사 + 냉동팀 -> 허용")
        fun createEmployee_manduWithFrozen_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "만두")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            service.createEmployee(10L, createRequest(professionalPromotionTeam = "프레시세일조_냉동"))
        }

        @Test
        @DisplayName("필드 검증 안 함 - 불일치 전문행사조로 등록 -> 검증 없이 저장 성공")
        fun createEmployee_noValidation_teamMismatch() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion(category = "라면")))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(professionalPromotionTeam = "프레시세일조_냉장"))
            assertThat(result.professionalPromotionTeam).isEqualTo("프레시세일조_냉장")
        }

        @Test
        @DisplayName("필드 검증 안 함 - 잘못된 근무상태로 등록 -> 검증 없이 저장 성공")
        fun createEmployee_noValidation_invalidWorkStatus() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(workStatus = "잘못된상태"))
            assertThat(result.workStatus).isEqualTo("잘못된상태")
        }

        @Test
        @DisplayName("workType1 null -> 기본값 '행사' 자동 세팅")
        fun createEmployee_workType1Null_defaultApplied() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(workType1 = null))
            assertThat(result.workType1).isEqualTo("행사")
        }

        @Test
        @DisplayName("workType1 전달 -> 기존 값 유지")
        fun createEmployee_workType1Provided_notOverridden() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(workType1 = "진열"))
            assertThat(result.workType1).isEqualTo("진열")
        }

        @Test
        @DisplayName("workStatus null -> 기본값 '근무' 자동 세팅")
        fun createEmployee_workStatusNull_defaultApplied() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(workStatus = null))
            assertThat(result.workStatus).isEqualTo("근무")
        }

        @Test
        @DisplayName("workStatus 전달 -> 기존 값 유지")
        fun createEmployee_workStatusProvided_notOverridden() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(workStatus = "연차"))
            assertThat(result.workStatus).isEqualTo("연차")
        }

        @Test
        @DisplayName("투입일만으로 즉시 추가 - 최소 필수 필드(scheduleDate)만 설정된 레코드 생성")
        fun createEmployee_scheduleDateOnly_success() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.createEmployee(10L, PromotionEmployeeRequest(scheduleDate = LocalDate.of(2026, 3, 15)))
            assertThat(result.promotionId).isEqualTo(10L)
            assertThat(result.employeeSfid).isNull()
            assertThat(result.employeeId).isNull()
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 15))
            assertThat(result.workType1).isEqualTo("행사")
            assertThat(result.workStatus).isEqualTo("근무")
        }

        @Test
        @DisplayName("투입일 범위 초과로 등록 -> 검증 없이 저장 성공")
        fun createEmployee_noValidation_scheduleDateOutOfRange() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(scheduleDate = LocalDate.of(2026, 3, 9)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 9))
        }

        @Test
        @DisplayName("투입일이 행사 시작일과 동일 (등록) -> 정상 등록")
        fun createEmployee_scheduleDateEqualsStart() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(scheduleDate = LocalDate.of(2026, 3, 10)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 10))
        }

        @Test
        @DisplayName("투입일이 행사 종료일과 동일 (등록) -> 정상 등록")
        fun createEmployee_scheduleDateEqualsEnd() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(scheduleDate = LocalDate.of(2026, 3, 20)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 20))
        }

        @Test
        @DisplayName("목표금액 자동 계산 - basePrice=5000, dailyTargetCount=10 -> targetAmount=50000")
        fun createEmployee_targetAmountCalculated() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                basePrice = 5000, dailyTargetCount = 10, targetAmount = 99999
            ))
            assertThat(result.targetAmount).isEqualTo(50000)
        }

        @Test
        @DisplayName("실적금액 자동 계산 - primaryProductAmount=30000, otherSalesAmount=5000 -> actualAmount=35000")
        fun createEmployee_actualAmountCalculated() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                primaryProductAmount = 30000, otherSalesAmount = 5000, actualAmount = 99999
            ))
            assertThat(result.actualAmount).isEqualTo(35000)
        }

        @Test
        @DisplayName("기준단가 null -> targetAmount=null")
        fun createEmployee_basePriceNull_targetAmountNull() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(basePrice = null, dailyTargetCount = 10))
            assertThat(result.targetAmount).isNull()
        }

        @Test
        @DisplayName("목표수량 null -> targetAmount=null")
        fun createEmployee_dailyTargetCountNull_targetAmountNull() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(basePrice = 5000, dailyTargetCount = null))
            assertThat(result.targetAmount).isNull()
        }

        @Test
        @DisplayName("목표수량 0 -> targetAmount=0")
        fun createEmployee_dailyTargetCountZero_targetAmountZero() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(basePrice = 5000, dailyTargetCount = 0))
            assertThat(result.targetAmount).isEqualTo(0)
        }

        @Test
        @DisplayName("실적 필드 모두 null -> actualAmount=null")
        fun createEmployee_bothAmountsNull_actualAmountNull() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                primaryProductAmount = null, otherSalesAmount = null
            ))
            assertThat(result.actualAmount).isNull()
        }

        @Test
        @DisplayName("실적 한쪽만 null -> actualAmount = non-null 값만 합산")
        fun createEmployee_oneAmountNull_actualAmountPartial() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                primaryProductAmount = 30000, otherSalesAmount = null
            ))
            assertThat(result.actualAmount).isEqualTo(30000)
        }

        @Test
        @DisplayName("employeeId로 sfid 해소 - User 존재 -> sfid 자동 설정")
        fun createEmployee_resolvesSfidFromEmployeeId() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = "20030117"))
            assertThat(result.employeeSfid).isEqualTo("a0B5g00000XYZabc")
            assertThat(result.employeeId).isEqualTo("20030117")
            assertThat(result.employeeName).isEqualTo("김여사")
        }

        @Test
        @DisplayName("존재하지 않는 employeeId -> sfid=null, name=null로 저장")
        fun createEmployee_unknownEmployeeId_sfidNull() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("99999999")).thenReturn(Optional.empty())
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = "99999999"))
            assertThat(result.employeeSfid).isNull()
            assertThat(result.employeeId).isEqualTo("99999999")
            assertThat(result.employeeName).isNull()
        }

        @Test
        @DisplayName("employeeId null -> sfid=null, employeeId=null")
        fun createEmployee_employeeIdNull() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = null))
            assertThat(result.employeeSfid).isNull()
            assertThat(result.employeeId).isNull()
        }

        @Test
        @DisplayName("sfid null인 User의 employeeId -> sfid=null, name 정상")
        fun createEmployee_sfidNullUser() {
            val sfidNullUser = User(id = 2L, sfid = null, employeeId = "00000002", name = "여사원테스트")
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(userRepository.findByEmployeeId("00000002")).thenReturn(Optional.of(sfidNullUser))
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = "00000002"))
            assertThat(result.employeeSfid).isNull()
            assertThat(result.employeeId).isEqualTo("00000002")
            assertThat(result.employeeName).isEqualTo("여사원테스트")
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
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 20)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 20))
        }

        @Test
        @DisplayName("마감 조원 비핵심필드 수정 -> 허용")
        fun updateEmployee_closedNonCritical_success() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            // work_type1만 변경 (비핵심필드)
            val result = service.updateEmployee(1L, 1L, createRequest(workType1 = "시음"))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("USER가 마감 조원 핵심필드(employeeId) 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun updateEmployee_closedCriticalField() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))

            assertThatThrownBy { service.updateEmployee(1L, 1L, createRequest(employeeId = "DIFFEREN")) }
                .isInstanceOf(ClosedEmployeeModificationException::class.java)
        }

        @Test
        @DisplayName("USER가 마감 조원 핵심필드(schedule_date) 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun updateEmployee_closedScheduleDate() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))

            assertThatThrownBy { service.updateEmployee(1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 4, 1))) }
                .isInstanceOf(ClosedEmployeeModificationException::class.java)
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 핵심필드(employeeId) 수정 -> 수정 허용")
        fun updateEmployee_adminClosedCriticalField_allowed() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser(appAuthority = "지점장")))
            whenever(userRepository.findByEmployeeId("DIFFEREN")).thenReturn(Optional.empty())
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(employeeId = "DIFFEREN"))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 핵심필드(투입일) 수정 -> 수정 허용")
        fun updateEmployee_adminClosedScheduleDate_allowed() {
            val pe = createPe(scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser(appAuthority = "지점장")))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 18)))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("확정 조원 핵심필드 변경 -> 스케줄 삭제 + schedule_id null")
        fun updateEmployee_criticalFieldChange_scheduleDeleted() {
            val pe = createPe(scheduleId = 100L)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("DIFFEREN")).thenReturn(Optional.empty())
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            service.updateEmployee(1L, 1L, createRequest(employeeId = "DIFFEREN"))

            verify(teamMemberScheduleRepository).deleteAllByIdIn(listOf(100L))
            assertThat(pe.scheduleId).isNull()
        }

        @Test
        @DisplayName("professionalPromotionTeam 변경 시 -> 스케줄 삭제 안 함")
        fun updateEmployee_teamChange_noScheduleDelete() {
            val pe = createPe(scheduleId = 100L)
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("DIFFEREN")).thenReturn(Optional.empty())
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            // team + employeeId 동시 변경 -> team 변경이 있으므로 스케줄 삭제 안 함
            service.updateEmployee(1L, 1L, createRequest(
                employeeId = "DIFFEREN",
                professionalPromotionTeam = "라면세일조B"
            ))

            verify(teamMemberScheduleRepository, never()).deleteAllByIdIn(any())
            assertThat(pe.scheduleId).isEqualTo(100L)
        }

        @Test
        @DisplayName("투입일이 행사 기간 이전 (수정) -> SCHEDULE_DATE_OUT_OF_RANGE")
        fun updateEmployee_scheduleDateBeforeStart() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))

            assertThatThrownBy { service.updateEmployee(1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 5))) }
                .isInstanceOf(ScheduleDateOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("workType3 null 수정 -> null로 저장 성공")
        fun updateEmployee_workType3Null_success() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(workType3 = null))
            assertThat(result.workType3).isNull()
        }

        @Test
        @DisplayName("workType3 빈 문자열 수정 -> null로 변환되어 저장")
        fun updateEmployee_workType3Empty_savedAsNull() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(workType3 = ""))
            assertThat(result.workType3).isNull()
        }

        @Test
        @DisplayName("employeeId null 수정 -> sfid=null로 저장 성공")
        fun updateEmployee_employeeIdNull_success() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(employeeId = null))
            assertThat(result.employeeSfid).isNull()
            assertThat(result.employeeId).isNull()
        }

        @Test
        @DisplayName("workType3 무효값 수정 -> InvalidWorkType3Exception")
        fun updateEmployee_workType3Invalid() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            assertThatThrownBy { service.updateEmployee(1L, 1L, createRequest(workType3 = "잘못된값")) }
                .isInstanceOf(InvalidWorkType3Exception::class.java)
        }

        @Test
        @DisplayName("workStatus null 수정 -> 기존 workStatus 값 유지")
        fun updateEmployee_workStatusNull_keepsExisting() {
            val pe = createPe(workStatus = "연차")
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(workStatus = null))
            assertThat(result.workStatus).isEqualTo("연차")
        }

        @Test
        @DisplayName("workType1 null 수정 -> 기존 workType1 값 유지")
        fun updateEmployee_workType1Null_keepsExisting() {
            val pe = createPe(workType1 = "시음")
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(workType1 = null))
            assertThat(result.workType1).isEqualTo("시음")
        }

        @Test
        @DisplayName("투입일이 행사 기간 이후 (수정) -> SCHEDULE_DATE_OUT_OF_RANGE")
        fun updateEmployee_scheduleDateAfterEnd() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))

            assertThatThrownBy { service.updateEmployee(1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 25))) }
                .isInstanceOf(ScheduleDateOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("수정 시 목표금액 자동 계산 - 요청값 무시")
        fun updateEmployee_targetAmountCalculated() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(
                basePrice = 3000, dailyTargetCount = 20, targetAmount = 99999
            ))
            assertThat(result.targetAmount).isEqualTo(60000)
        }

        @Test
        @DisplayName("수정 시 실적금액 자동 계산 - 요청값 무시")
        fun updateEmployee_actualAmountCalculated() {
            val pe = createPe()
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup()

            val result = service.updateEmployee(1L, 1L, createRequest(
                primaryProductAmount = 20000, otherSalesAmount = 10000, actualAmount = 99999
            ))
            assertThat(result.actualAmount).isEqualTo(30000)
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
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
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
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(createUser()))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            stubRollup(targetSum = 150000, actualSum = 110000)

            service.updateEmployee(1L, 1L, createRequest(targetAmount = 50000, actualAmount = 30000))

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

            verify(teamMemberScheduleRepository).deleteAllByIdIn(listOf(100L))
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

    @Nested
    @DisplayName("batchUpdateEmployees - 일괄 수정")
    inner class BatchUpdateEmployeesTests {

        @Test
        @DisplayName("정상 일괄 수정 - 2건 수정 성공")
        fun batchUpdate_success() {
            val pe1 = createPe(id = 1L, scheduleDate = LocalDate.of(2026, 3, 15))
            val pe2 = createPe(id = 2L, scheduleDate = LocalDate.of(2026, 3, 16))
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe1))
            whenever(promotionEmployeeRepository.findById(2L)).thenReturn(Optional.of(pe2))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe1, pe2))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, scheduleDate = LocalDate.of(2026, 3, 18)),
                createBatchItem(id = 2L, scheduleDate = LocalDate.of(2026, 3, 19))
            ))

            val result = service.batchUpdateEmployees(10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(2)
        }

        @Test
        @DisplayName("items 빈 배열 -> INVALID_PARAMETER")
        fun batchUpdate_emptyItems() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))

            assertThatThrownBy {
                service.batchUpdateEmployees(10L, 1L, BatchUpdatePromotionEmployeeRequest(emptyList()))
            }.isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("items 내 id 중복 -> INVALID_PARAMETER")
        fun batchUpdate_duplicateIds() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L),
                createBatchItem(id = 1L)
            ))

            assertThatThrownBy {
                service.batchUpdateEmployees(10L, 1L, request)
            }.isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("수정 대상 미존재 -> BATCH_VALIDATION_FAILED")
        fun batchUpdate_notFound() {
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(999L)).thenReturn(Optional.empty())

            val request = BatchUpdatePromotionEmployeeRequest(listOf(createBatchItem(id = 999L)))

            assertThatThrownBy {
                service.batchUpdateEmployees(10L, 1L, request)
            }.isInstanceOf(BatchValidationException::class.java)
        }

        @Test
        @DisplayName("에러 일괄 반환 - 2건 모두 실패")
        fun batchUpdate_multipleErrors() {
            val pe1 = createPe(id = 1L)
            val pe2 = createPe(id = 2L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe1))
            whenever(promotionEmployeeRepository.findById(2L)).thenReturn(Optional.of(pe2))

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, scheduleDate = LocalDate.of(2026, 5, 1)),  // 범위 초과
                createBatchItem(id = 2L, workStatus = "잘못된상태")  // 잘못된 근무상태
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(10L, 1L, request)
            }
            assertThat(ex.errors).hasSize(2)
            assertThat(ex.errors[0].errorCode).isEqualTo("SCHEDULE_DATE_OUT_OF_RANGE")
            assertThat(ex.errors[1].errorCode).isEqualTo("INVALID_WORK_STATUS")
        }

        @Test
        @DisplayName("일괄 수정 - workStatus/workType1 null -> 기존값 유지")
        fun batchUpdate_nullWorkStatusAndType1_keepsExisting() {
            val pe = createPe(id = 1L, workStatus = "연차", workType1 = "시음")
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workStatus = null, workType1 = null)
            ))

            val result = service.batchUpdateEmployees(10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.workStatus).isEqualTo("연차")
            assertThat(pe.workType1).isEqualTo("시음")
        }

        @Test
        @DisplayName("일괄 수정 - workStatus 무효값 -> INVALID_WORK_STATUS (null은 허용)")
        fun batchUpdate_invalidWorkStatusButNullAllowed() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workStatus = "출장")
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("INVALID_WORK_STATUS")
        }

        @Test
        @DisplayName("투입일 범위 초과 -> SCHEDULE_DATE_OUT_OF_RANGE 에러 수집")
        fun batchUpdate_scheduleDateOutOfRange() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, scheduleDate = LocalDate.of(2026, 5, 1))
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("SCHEDULE_DATE_OUT_OF_RANGE")
            assertThat(ex.errors[0].itemIndex).isEqualTo(0)
        }

        @Test
        @DisplayName("비관리자 마감 행사사원 핵심필드 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun batchUpdate_closedEmployeeModification() {
            val pe = createPe(id = 1L, scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(emptyList())
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = "DIFFEREN")
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("CLOSED_EMPLOYEE_MODIFICATION")
        }

        @Test
        @DisplayName("관리자 마감 행사사원 핵심필드 수정 -> 수정 허용")
        fun batchUpdate_adminClosedEmployeeModification_allowed() {
            val pe = createPe(id = 1L, scheduleId = 100L, promoCloseByTm = true)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser(appAuthority = "지점장")))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(emptyList())
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = "DIFFEREN")
            ))

            val result = service.batchUpdateEmployees(10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("핵심필드 변경 시 스케줄 삭제")
        fun batchUpdate_criticalFieldChange_scheduleDeleted() {
            val pe = createPe(id = 1L, scheduleId = 100L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(emptyList())
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = "NEW_EMP")
            ))

            service.batchUpdateEmployees(10L, 1L, request)

            verify(teamMemberScheduleRepository).deleteAllByIdIn(listOf(100L))
            assertThat(pe.scheduleId).isNull()
        }

        @Test
        @DisplayName("workType3 null 일괄 수정 -> null로 저장 성공")
        fun batchUpdate_workType3Null_success() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType3 = null)
            ))

            val result = service.batchUpdateEmployees(10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.workType3).isNull()
        }

        @Test
        @DisplayName("workType3 빈 문자열 일괄 수정 -> null로 변환되어 저장")
        fun batchUpdate_workType3Empty_savedAsNull() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType3 = "")
            ))

            val result = service.batchUpdateEmployees(10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.workType3).isNull()
        }

        @Test
        @DisplayName("일괄수정 - 기존값도 null이면 workType1 '행사' 보정")
        fun batchUpdate_workType1BothNull_defaultApplied() {
            val pe = createPe(id = 1L, workType1 = null)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType1 = null)
            ))

            service.batchUpdateEmployees(10L, 1L, request)
            assertThat(pe.workType1).isEqualTo("행사")
        }

        @Test
        @DisplayName("일괄수정 - 기존값도 null이면 workStatus '근무' 보정")
        fun batchUpdate_workStatusBothNull_defaultApplied() {
            val pe = createPe(id = 1L, workStatus = null)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workStatus = null)
            ))

            service.batchUpdateEmployees(10L, 1L, request)
            assertThat(pe.workStatus).isEqualTo("근무")
        }

        @Test
        @DisplayName("employeeId null 일괄 수정 -> null로 저장 성공")
        fun batchUpdate_employeeIdNull_success() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = null)
            ))

            val result = service.batchUpdateEmployees(10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.employeeSfid).isNull()
            assertThat(pe.employeeId).isNull()
        }

        @Test
        @DisplayName("workType3 무효값 일괄 수정 -> INVALID_WORK_TYPE3 에러")
        fun batchUpdate_workType3Invalid() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType3 = "잘못된값")
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("INVALID_WORK_TYPE3")
        }

        @Test
        @DisplayName("일괄 수정 시 목표금액/실적금액 자동 계산 - 요청값 무시")
        fun batchUpdate_calculatedFields() {
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(createPromotion()))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(
                    id = 1L, basePrice = 5000, dailyTargetCount = 10,
                    targetAmount = 99999, actualAmount = 99999,
                    primaryProductAmount = 30000, otherSalesAmount = 5000
                )
            ))

            service.batchUpdateEmployees(10L, 1L, request)
            assertThat(pe.targetAmount).isEqualTo(50000)
            assertThat(pe.actualAmount).isEqualTo(35000)
        }

        @Test
        @DisplayName("롤업 재계산 - 수정 후 행사마스터 합계 갱신")
        fun batchUpdate_rollupRecalculation() {
            val promotion = createPromotion()
            val pe = createPe(id = 1L)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser()))
            whenever(userRepository.findByEmployeeIdIn(any())).thenReturn(listOf(createUser()))
            whenever(promotionEmployeeRepository.findById(1L)).thenReturn(Optional.of(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>()))
                .thenAnswer { it.getArgument<PromotionEmployee>(0) }
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(10L))
                .thenReturn(listOf(pe))
            stubRollup(targetSum = 200000, actualSum = 150000)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, targetAmount = 200000, actualAmount = 150000)
            ))

            service.batchUpdateEmployees(10L, 1L, request)

            assertThat(promotion.targetAmount).isEqualTo(200000)
            assertThat(promotion.actualAmount).isEqualTo(150000)
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
        employeeId: String = "20030117",
        scheduleDate: LocalDate = LocalDate.of(2026, 3, 15), scheduleId: Long? = null,
        promoCloseByTm: Boolean = false, workStatus: String? = "근무", workType1: String? = "시식"
    ) = PromotionEmployee(
        id = id, promotionId = promotionId, employeeSfid = employeeSfid, employeeId = employeeId,
        scheduleDate = scheduleDate,
        workStatus = workStatus, workType1 = workType1, workType3 = "고정", workType4 = "냉장",
        professionalPromotionTeam = "라면세일조", basePrice = 1500, dailyTargetCount = 100,
        scheduleId = scheduleId, promoCloseByTm = promoCloseByTm
    )

    private fun createUser(appAuthority: String? = null) = User(id = 1L, sfid = "a0B5g00000XYZabc", employeeId = "20030117", name = "김여사").also {
        it.appAuthority = appAuthority
    }

    private fun createBatchItem(
        id: Long = 1L, employeeId: String? = "20030117", scheduleDate: LocalDate = LocalDate.of(2026, 3, 15),
        workStatus: String? = "근무", workType1: String? = "시식", workType3: String? = "고정",
        workType4: String? = "냉장", professionalPromotionTeam: String? = "라면세일조",
        basePrice: Long? = 1500, dailyTargetCount: Int? = 100,
        targetAmount: Long? = 0, actualAmount: Long? = 0,
        primaryProductAmount: Long? = null, otherSalesAmount: Long? = null
    ) = BatchUpdatePromotionEmployeeItem(
        id = id, employeeId = employeeId, scheduleDate = scheduleDate, workStatus = workStatus,
        workType1 = workType1, workType3 = workType3, workType4 = workType4,
        professionalPromotionTeam = professionalPromotionTeam, basePrice = basePrice,
        dailyTargetCount = dailyTargetCount, targetAmount = targetAmount, actualAmount = actualAmount,
        primaryProductAmount = primaryProductAmount, otherSalesAmount = otherSalesAmount
    )

    private fun createRequest(
        employeeId: String? = "20030117", scheduleDate: LocalDate = LocalDate.of(2026, 3, 15),
        workStatus: String? = "근무", workType1: String? = "시식", workType3: String? = "고정",
        workType4: String? = "냉장", professionalPromotionTeam: String? = "라면세일조",
        basePrice: Long? = 1500, dailyTargetCount: Int? = 100,
        targetAmount: Long? = 0, actualAmount: Long? = 0,
        primaryProductAmount: Long? = null, otherSalesAmount: Long? = null
    ) = PromotionEmployeeRequest(
        employeeId = employeeId, scheduleDate = scheduleDate, workStatus = workStatus,
        workType1 = workType1, workType3 = workType3, workType4 = workType4,
        professionalPromotionTeam = professionalPromotionTeam, basePrice = basePrice,
        dailyTargetCount = dailyTargetCount, targetAmount = targetAmount, actualAmount = actualAmount,
        primaryProductAmount = primaryProductAmount, otherSalesAmount = otherSalesAmount
    )
}
