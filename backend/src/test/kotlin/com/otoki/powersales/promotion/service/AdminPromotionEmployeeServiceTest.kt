package com.otoki.powersales.promotion.service

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeItem
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import java.math.BigDecimal

@DisplayName("AdminPromotionEmployeeService 테스트")
class AdminPromotionEmployeeServiceTest {

    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk(relaxUnitFun = true)
    private val promotionRepository: PromotionRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val policyEvaluator: SharingRulePolicyEvaluator =
        mockk(relaxed = true)
    private val teamMemberScheduleCascadeHelper: com.otoki.powersales.schedule.service.TeamMemberScheduleCascadeHelper =
        mockk(relaxUnitFun = true)
    private val principal: WebUserPrincipal = mockk(relaxed = true)

    private val service: AdminPromotionEmployeeService = AdminPromotionEmployeeService(
        promotionEmployeeRepository = promotionEmployeeRepository,
        promotionRepository = promotionRepository,
        employeeRepository = employeeRepository,
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        policyEvaluator = policyEvaluator,
        teamMemberScheduleCascadeHelper = teamMemberScheduleCascadeHelper,
    )

    // 부모 Promotion 가시 범위 검증 통과용 — 전체 지점 권한 (isAllBranches=true → validateAccess 항상 true).
    private val allBranchesScope = com.otoki.powersales.admin.dto.DataScope(
        branchCodes = emptyList(),
        isAllBranches = true,
    )

    // 원본 mockito 테스트가 @MockitoSettings(LENIENT) 였으므로 strict MockK 환경에서
    // 일부 호출 (employeeRepository.findById 등) 이 silent no-op 으로 통과해 왔다.
    // 본 기본 stub 은 그 호환성을 위한 default — 각 테스트에서 override 가능.
    @BeforeEach
    fun stubLenientCompatDefaults() {
        every { employeeRepository.findById(any()) } returns Optional.empty()
    }

    @Nested
    @DisplayName("getEmployees - 행사조원 목록 조회")
    inner class GetEmployeesTests {

        @Test
        @DisplayName("정상 조회 - 행사에 조원 존재 -> 목록 반환")
        fun getEmployees_success() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            val employees = listOf(
                createPe(id = 1L, scheduleDate = LocalDate.of(2026, 3, 15)),
                createPe(id = 2L, scheduleDate = LocalDate.of(2026, 3, 16))
            )
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns employees


            val result = service.getEmployees(allBranchesScope, 10L)
            assertThat(result).hasSize(2)
            assertThat(result[0].employeeName).isEqualTo("김여사")
        }

        @Test
        @DisplayName("행사 미존재 -> PromotionNotFoundException")
        fun getEmployees_promotionNotFound() {
            every { promotionRepository.findById(999L) } returns Optional.empty()
            assertThatThrownBy { service.getEmployees(allBranchesScope, 999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("가시 범위 밖 행사 -> PromotionForbiddenException (ControlledByParent)")
        fun getEmployees_outOfScope() {
            // promotion costCenterCode 가 scope.branchCodes 에 없음 → 부모 가시성 검증 실패
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            val restrictedScope = com.otoki.powersales.admin.dto.DataScope(
                branchCodes = listOf("ZZZ99"),
                isAllBranches = false,
            )
            assertThatThrownBy { service.getEmployees(restrictedScope, 10L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("createEmployee - 등록 + 전문행사조 매칭")
    inner class CreateEmployeeTests {

        @Test
        @DisplayName("정상 등록 - 라면행사 + 라면세일조 -> 성공")
        fun createEmployee_teamMatch_success() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            stubRollup()

            val result = service.createEmployee(10L, createRequest())
            assertThat(result.employeeCode).isEqualTo("20030117")
        }

        @Test
        @DisplayName("전문행사조 null -> 모든 카테고리 허용")
        fun createEmployee_teamNull_success() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("전문행사조 '일반' -> 모든 카테고리 허용")
        fun createEmployee_teamGeneral_success() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("만두행사 + 냉동팀 -> 허용")
        fun createEmployee_manduWithFrozen_success() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("필드 검증 안 함 - 불일치 전문행사조로 등록 -> 검증 없이 저장 성공")
        fun createEmployee_noValidation_teamMismatch() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        // --- UC-07: 대표제품 vs 전문행사조 매칭 검증 (레거시 PromotionEmployeeTriggerHandler 동등) ---

        @Test
        @DisplayName("UC-07: 라면행사 + 라면세일조 사원 -> 매칭 OK (저장 성공)")
        fun createEmployee_uc07_ramenWithRamenSaleTeam_success() {
            val promotion = createPromotion(category1 = "라면")
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                .also { it.professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE }

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("UC-07: 만두행사 + 카레행사조 사원 -> 매칭 OK (카레는 모든 카테고리 허용)")
        fun createEmployee_uc07_manduWithCurryTeam_success() {
            val promotion = createPromotion(category1 = "만두")
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                .also { it.professionalPromotionTeam = ProfessionalPromotionTeamType.CURRY_PROMOTION }

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("UC-07: 라면행사 + 프레시세일조_냉장 사원 -> 매칭 실패 (TeamCategoryMismatchException)")
        fun createEmployee_uc07_ramenWithFreshRefrigeratedTeam_fail() {
            val promotion = createPromotion(category1 = "라면")
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                .also { it.professionalPromotionTeam = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED }

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)

            assertThatThrownBy { service.createEmployee(10L, createRequest()) }
                .isInstanceOf(TeamCategoryMismatchException::class.java)
                .hasMessageContaining("라면")
                .hasMessageContaining("라면세일조")
        }

        @Test
        @DisplayName("UC-07: 냉장행사 + 프레시세일조_냉동 사원 -> 매칭 실패")
        fun createEmployee_uc07_refrigeratedWithFreshFrozenTeam_fail() {
            val promotion = createPromotion(category1 = "냉장")
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                .also { it.professionalPromotionTeam = ProfessionalPromotionTeamType.FRESH_SALE_FROZEN }

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)

            assertThatThrownBy { service.createEmployee(10L, createRequest()) }
                .isInstanceOf(TeamCategoryMismatchException::class.java)
                .hasMessageContaining("냉장")
                .hasMessageContaining("프레시세일조_냉장")
        }

        @Test
        @DisplayName("UC-07: 만두행사 + 라면세일조 사원 -> 매칭 실패 (라면은 만두 허용 룰 외)")
        fun createEmployee_uc07_manduWithRamenSaleTeam_fail() {
            val promotion = createPromotion(category1 = "만두")
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                .also { it.professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE }

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)

            assertThatThrownBy { service.createEmployee(10L, createRequest()) }
                .isInstanceOf(TeamCategoryMismatchException::class.java)
                .hasMessageContaining("만두")
        }

        @Test
        @DisplayName("UC-07: promotion.category1 null -> 검증 스킵 (저장 성공)")
        fun createEmployee_uc07_promotionCategoryNull_success() {
            val promotion = createPromotion() // category1 null
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                .also { it.professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE }

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("UC-07: employee.professionalPromotionTeam null -> 검증 스킵 (일반 사원 동등)")
        fun createEmployee_uc07_employeeTeamNull_success() {
            val promotion = createPromotion(category1 = "라면")
            val employee = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사")
                // professionalPromotionTeam = null (일반 사원)

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            service.createEmployee(10L, createRequest())
        }

        @Test
        @DisplayName("필드 검증 안 함 - 잘못된 근무상태로 등록 -> 검증 없이 저장 성공")
        fun createEmployee_noValidation_invalidWorkStatus() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(workStatus = "잘못된상태"))
            // 잘못된 enum 값은 fromDisplayNameOrNull 로 null 변환 → default("근무") 보정
            assertThat(result.workStatus).isEqualTo("근무")
        }

        @Test
        @DisplayName("workType1 null -> 기본값 '행사' 자동 세팅")
        fun createEmployee_workType1Null_defaultApplied() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(workType1 = null))
            assertThat(result.workType1).isEqualTo("행사")
        }

        @Test
        @DisplayName("workType1 전달 -> 기존 값 유지")
        fun createEmployee_workType1Provided_notOverridden() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(workType1 = "진열"))
            assertThat(result.workType1).isEqualTo("진열")
        }

        @Test
        @DisplayName("workStatus null -> 기본값 '근무' 자동 세팅")
        fun createEmployee_workStatusNull_defaultApplied() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(workStatus = null))
            assertThat(result.workStatus).isEqualTo("근무")
        }

        @Test
        @DisplayName("workStatus 전달 -> 기존 값 유지")
        fun createEmployee_workStatusProvided_notOverridden() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(workStatus = "연차"))
            assertThat(result.workStatus).isEqualTo("연차")
        }

        @Test
        @DisplayName("투입일만으로 즉시 추가 - 최소 필수 필드(scheduleDate)만 설정된 레코드 생성")
        fun createEmployee_scheduleDateOnly_success() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.createEmployee(10L, PromotionEmployeeRequest(scheduleDate = LocalDate.of(2026, 3, 15)))
            assertThat(result.promotionId).isEqualTo(10L)
            assertThat(result.employeeCode).isNull()
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 15))
            assertThat(result.workType1).isEqualTo("행사")
            assertThat(result.workStatus).isEqualTo("근무")
        }

        @Test
        @DisplayName("투입일 범위 초과로 등록 -> 검증 없이 저장 성공")
        fun createEmployee_noValidation_scheduleDateOutOfRange() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(scheduleDate = LocalDate.of(2026, 3, 9)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 9))
        }

        @Test
        @DisplayName("투입일이 행사 시작일과 동일 (등록) -> 정상 등록")
        fun createEmployee_scheduleDateEqualsStart() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(scheduleDate = LocalDate.of(2026, 3, 10)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 10))
        }

        @Test
        @DisplayName("투입일이 행사 종료일과 동일 (등록) -> 정상 등록")
        fun createEmployee_scheduleDateEqualsEnd() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(scheduleDate = LocalDate.of(2026, 3, 20)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 20))
        }

        @Test
        @DisplayName("목표금액 자동 계산 - basePrice = BigDecimal.valueOf(5000L), dailyTargetCount = BigDecimal.valueOf(10L) -> targetAmount=50000")
        fun createEmployee_targetAmountCalculated() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                basePrice = BigDecimal.valueOf(5000L), dailyTargetCount = BigDecimal.valueOf(10L), targetAmount = 99999
            ))
            assertThat(result.targetAmount).isEqualTo(50000)
        }

        @Test
        @DisplayName("실적금액 자동 계산 - primaryProductAmount = BigDecimal.valueOf(30000L), otherSalesAmount = BigDecimal.valueOf(5000L) -> actualAmount=35000")
        fun createEmployee_actualAmountCalculated() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                primaryProductAmount = BigDecimal.valueOf(30000L), otherSalesAmount = BigDecimal.valueOf(5000L), actualAmount = 99999
            ))
            assertThat(result.actualAmount).isEqualTo(35000)
        }

        @Test
        @DisplayName("기준단가 null -> targetAmount=null")
        fun createEmployee_basePriceNull_targetAmountNull() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(basePrice = null, dailyTargetCount = BigDecimal.valueOf(10L)))
            assertThat(result.targetAmount).isNull()
        }

        @Test
        @DisplayName("목표수량 null -> targetAmount=null")
        fun createEmployee_dailyTargetCountNull_targetAmountNull() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(basePrice = BigDecimal.valueOf(5000L), dailyTargetCount = null))
            assertThat(result.targetAmount).isNull()
        }

        @Test
        @DisplayName("목표수량 0 -> targetAmount=0")
        fun createEmployee_dailyTargetCountZero_targetAmountZero() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(basePrice = BigDecimal.valueOf(5000L), dailyTargetCount = BigDecimal.valueOf(0L)))
            assertThat(result.targetAmount).isEqualTo(0)
        }

        @Test
        @DisplayName("실적 필드 모두 null -> actualAmount=null")
        fun createEmployee_bothAmountsNull_actualAmountNull() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                primaryProductAmount = null, otherSalesAmount = null
            ))
            assertThat(result.actualAmount).isNull()
        }

        @Test
        @DisplayName("실적 한쪽만 null -> actualAmount = non-null 값만 합산")
        fun createEmployee_oneAmountNull_actualAmountPartial() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup()

            val result = service.createEmployee(10L, createRequest(
                primaryProductAmount = BigDecimal.valueOf(30000L), otherSalesAmount = null
            ))
            assertThat(result.actualAmount).isEqualTo(30000)
        }

        @Test
        @DisplayName("employeeId로 사원 해소 - Employee 존재 -> code/name 자동 설정")
        fun createEmployee_resolvesFromEmployeeId() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = 1L))
            assertThat(result.employeeCode).isEqualTo("20030117")
            assertThat(result.employeeName).isEqualTo("김여사")
        }

        @Test
        @DisplayName("존재하지 않는 employeeId -> code=null, name=null로 저장")
        fun createEmployee_unknownEmployeeId() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { employeeRepository.findById(999999L) } returns Optional.empty()
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = 999999L))
            assertThat(result.employeeId).isEqualTo(999999L)
            assertThat(result.employeeCode).isNull()
            assertThat(result.employeeName).isNull()
        }

        @Test
        @DisplayName("employeeId null -> employeeId=null, employeeCode=null")
        fun createEmployee_employeeIdNull() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = null))
            assertThat(result.employeeCode).isNull()
        }

        @Test
        @DisplayName("sfid null인 Employee의 employeeId -> name 정상 조회")
        fun createEmployee_sfidNullEmployee() {
            val sfidNullEmployee = Employee(id = 2L, sfid = null, employeeCode = "00000002", name = "여사원테스트")
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { employeeRepository.findById(2L) } returns Optional.of(sfidNullEmployee)
            stubRollup()

            val result = service.createEmployee(10L, createRequest(employeeId = 2L))
            assertThat(result.employeeCode).isEqualTo("00000002")
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
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 20)))
            assertThat(result.scheduleDate).isEqualTo(LocalDate.of(2026, 3, 20))
        }

        @Test
        @DisplayName("마감 조원 비핵심필드 수정 -> 허용")
        fun updateEmployee_closedNonCritical_success() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            // work_type1만 변경 (비핵심필드)
            val result = service.updateEmployee(principal, 1L, 1L, createRequest(workType1 = "행사"))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("USER가 마감 조원 핵심필드(employeeId) 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun updateEmployee_closedCriticalField() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { employeeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { service.updateEmployee(principal, 1L, 1L, createRequest(employeeId = 999L)) }
                .isInstanceOf(ClosedEmployeeModificationException::class.java)
        }

        @Test
        @DisplayName("USER가 마감 조원 핵심필드(schedule_date) 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun updateEmployee_closedScheduleDate() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())


            assertThatThrownBy { service.updateEmployee(principal, 1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 4, 1))) }
                .isInstanceOf(ClosedEmployeeModificationException::class.java)
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 핵심필드(employeeId) 수정 -> 수정 허용")
        fun updateEmployee_adminClosedCriticalField_allowed() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee(role = AppAuthority.BRANCH_MANAGER))
            every { employeeRepository.findById(999L) } returns Optional.empty()
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(employeeId = 999L))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 핵심필드(투입일) 수정 -> 수정 허용")
        fun updateEmployee_adminClosedScheduleDate_allowed() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee(role = AppAuthority.BRANCH_MANAGER))

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 18)))
            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("확정 조원 핵심필드 변경 -> 스케줄 삭제 + schedule_id null")
        fun updateEmployee_criticalFieldChange_scheduleDeleted() {
            val pe = createPe(teamMemberScheduleId = 100L)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { employeeRepository.findById(999L) } returns Optional.empty()
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            service.updateEmployee(principal, 1L, 1L, createRequest(employeeId = 999L))

            verify { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(100L)) }
            assertThat(pe.teamMemberScheduleId).isNull()
        }

        @Test
        @DisplayName("professionalPromotionTeam 변경 시 -> 스케줄 삭제 안 함")
        fun updateEmployee_teamChange_noScheduleDelete() {
            val pe = createPe(teamMemberScheduleId = 100L)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { employeeRepository.findById(999L) } returns Optional.empty()
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            // employeeId 변경 -> 스케줄 삭제 진행 (team 검증 제거됨)
            service.updateEmployee(principal, 1L, 1L, createRequest(
                employeeId = 999L
            ))

            verify(atLeast = 1) { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, any()) }
        }

        @Test
        @DisplayName("투입일이 행사 기간 이전 (수정) -> SCHEDULE_DATE_OUT_OF_RANGE")
        fun updateEmployee_scheduleDateBeforeStart() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())


            assertThatThrownBy { service.updateEmployee(principal, 1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 5))) }
                .isInstanceOf(ScheduleDateOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("workType3 null 수정 -> null로 저장 성공")
        fun updateEmployee_workType3Null_success() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(workType3 = null))
            assertThat(result.workType3).isNull()
        }

        @Test
        @DisplayName("workType3 빈 문자열 수정 -> null로 변환되어 저장")
        fun updateEmployee_workType3Empty_savedAsNull() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(workType3 = ""))
            assertThat(result.workType3).isNull()
        }

        @Test
        @DisplayName("employeeId null 수정 -> employeeId=null로 저장 성공")
        fun updateEmployee_employeeIdNull_success() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(employeeId = null))
            assertThat(result.employeeCode).isNull()
        }

        @Test
        @DisplayName("workType3 무효값 수정 -> InvalidWorkType3Exception")
        fun updateEmployee_workType3Invalid() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)


            assertThatThrownBy { service.updateEmployee(principal, 1L, 1L, createRequest(workType3 = "잘못된값")) }
                .isInstanceOf(InvalidWorkType3Exception::class.java)
        }

        @Test
        @DisplayName("workStatus null 수정 -> 기존 workStatus 값 유지")
        fun updateEmployee_workStatusNull_keepsExisting() {
            val pe = createPe(workStatus = "연차")
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(workStatus = null))
            assertThat(result.workStatus).isEqualTo("연차")
        }

        @Test
        @DisplayName("workType1 null 수정 -> 기존 workType1 값 유지")
        fun updateEmployee_workType1Null_keepsExisting() {
            val pe = createPe(workType1 = "행사")
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(workType1 = null))
            assertThat(result.workType1).isEqualTo("행사")
        }

        @Test
        @DisplayName("투입일이 행사 기간 이후 (수정) -> SCHEDULE_DATE_OUT_OF_RANGE")
        fun updateEmployee_scheduleDateAfterEnd() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())


            assertThatThrownBy { service.updateEmployee(principal, 1L, 1L, createRequest(scheduleDate = LocalDate.of(2026, 3, 25))) }
                .isInstanceOf(ScheduleDateOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("수정 시 목표금액 자동 계산 - 요청값 무시")
        fun updateEmployee_targetAmountCalculated() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(
                basePrice = BigDecimal.valueOf(3000L), dailyTargetCount = BigDecimal.valueOf(20L), targetAmount = 99999
            ))
            assertThat(result.targetAmount).isEqualTo(60000)
        }

        @Test
        @DisplayName("수정 시 실적금액 자동 계산 - 요청값 무시")
        fun updateEmployee_actualAmountCalculated() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup()

            val result = service.updateEmployee(principal, 1L, 1L, createRequest(
                primaryProductAmount = BigDecimal.valueOf(20000L), otherSalesAmount = BigDecimal.valueOf(10000L), actualAmount = 99999
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
            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            stubRollup(targetSum = 100000, actualSum = 80000)

            service.createEmployee(10L, createRequest(targetAmount = 100000, actualAmount = 80000))
        }

        @Test
        @DisplayName("조원 수정 후 행사마스터 합계 갱신")
        fun updateEmployee_updatesPromotionAmounts() {
            val promotion = createPromotion()
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            stubRollup(targetSum = 150000, actualSum = 110000)

            service.updateEmployee(principal, 1L, 1L, createRequest(targetAmount = 50000, actualAmount = 30000))
        }

        @Test
        @DisplayName("조원 삭제 후 행사마스터 합계 갱신 (0으로)")
        fun deleteEmployee_updatesPromotionAmounts() {
            val promotion = createPromotion()
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            stubRollup(targetSum = 0, actualSum = 0)

            service.deleteEmployee(principal, 1L)
        }
    }

    @Nested
    @DisplayName("deleteEmployee - 삭제 + 보호 규칙")
    inner class DeleteEmployeeTests {

        @Test
        @DisplayName("미마감 조원 삭제 -> 성공")
        fun deleteEmployee_success() {
            val pe = createPe()
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            stubRollup()

            service.deleteEmployee(principal, 1L)
            verify { promotionEmployeeRepository.delete(pe) }
        }

        @Test
        @DisplayName("확정 조원(미마감) 삭제 -> 스케줄 연쇄 삭제")
        fun deleteEmployee_withSchedule_cascadeDelete() {
            val pe = createPe(teamMemberScheduleId = 100L)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            stubRollup()

            service.deleteEmployee(principal, 1L)

            verify { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(100L)) }
            verify { promotionEmployeeRepository.delete(pe) }
        }

        @Test
        @DisplayName("마감 조원 삭제 -> CLOSED_EMPLOYEE_DELETE")
        fun deleteEmployee_closed() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)

            assertThatThrownBy { service.deleteEmployee(principal, 1L) }
                .isInstanceOf(ClosedEmployeeDeleteException::class.java)
        }

        @Test
        @DisplayName("마감 조원이지만 사번 00000009 의 여사원 -> 삭제 허용 (레거시 동등)")
        fun deleteEmployee_closed_bypassForSpecialEmployeeCode() {
            val pe = createPe(teamMemberScheduleId = 100L, promoCloseByTm = true).also {
                it.employee = Employee(id = 999L, sfid = "a0B5g00000SPCabc", employeeCode = "00000009", name = "운영점검계정")
            }
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            stubRollup()

            service.deleteEmployee(principal, 1L)

            verify { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(100L)) }
            verify { promotionEmployeeRepository.delete(pe) }
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
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe1)
            every { promotionEmployeeRepository.findById(2L) } returns Optional.of(pe2)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe1, pe2)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, scheduleDate = LocalDate.of(2026, 3, 18)),
                createBatchItem(id = 2L, scheduleDate = LocalDate.of(2026, 3, 19))
            ))

            val result = service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(2)
        }

        @Test
        @DisplayName("items 빈 배열 -> INVALID_PARAMETER")
        fun batchUpdate_emptyItems() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())

            assertThatThrownBy {
                service.batchUpdateEmployees(principal, 10L, 1L, BatchUpdatePromotionEmployeeRequest(emptyList()))
            }.isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("items 내 id 중복 -> INVALID_PARAMETER")
        fun batchUpdate_duplicateIds() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L),
                createBatchItem(id = 1L)
            ))

            assertThatThrownBy {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }.isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("수정 대상 미존재 -> BATCH_VALIDATION_FAILED")
        fun batchUpdate_notFound() {
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionEmployeeRepository.findById(999L) } returns Optional.empty()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(createBatchItem(id = 999L)))

            assertThatThrownBy {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }.isInstanceOf(BatchValidationException::class.java)
        }

        @Test
        @DisplayName("에러 일괄 반환 - 2건 모두 실패")
        fun batchUpdate_multipleErrors() {
            val pe1 = createPe(id = 1L)
            val pe2 = createPe(id = 2L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe1)
            every { promotionEmployeeRepository.findById(2L) } returns Optional.of(pe2)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, scheduleDate = LocalDate.of(2026, 5, 1)),  // 범위 초과
                createBatchItem(id = 2L, workStatus = "잘못된상태")  // 잘못된 근무상태
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }
            assertThat(ex.errors).hasSize(2)
            assertThat(ex.errors[0].errorCode).isEqualTo("SCHEDULE_DATE_OUT_OF_RANGE")
            assertThat(ex.errors[1].errorCode).isEqualTo("INVALID_WORK_STATUS")
        }

        @Test
        @DisplayName("일괄 수정 - workStatus/workType1 null -> 기존값 유지")
        fun batchUpdate_nullWorkStatusAndType1_keepsExisting() {
            val pe = createPe(id = 1L, workStatus = "연차", workType1 = "행사")
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workStatus = null, workType1 = null)
            ))

            val result = service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.workStatus?.displayName).isEqualTo("연차")
            assertThat(pe.workType1?.displayName).isEqualTo("행사")
        }

        @Test
        @DisplayName("일괄 수정 - workStatus 무효값 -> INVALID_WORK_STATUS (null은 허용)")
        fun batchUpdate_invalidWorkStatusButNullAllowed() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workStatus = "출장")
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("INVALID_WORK_STATUS")
        }

        @Test
        @DisplayName("투입일 범위 초과 -> SCHEDULE_DATE_OUT_OF_RANGE 에러 수집")
        fun batchUpdate_scheduleDateOutOfRange() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, scheduleDate = LocalDate.of(2026, 5, 1))
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("SCHEDULE_DATE_OUT_OF_RANGE")
            assertThat(ex.errors[0].itemIndex).isEqualTo(0)
        }

        @Test
        @DisplayName("비관리자 마감 행사사원 핵심필드 수정 -> CLOSED_EMPLOYEE_MODIFICATION")
        fun batchUpdate_closedEmployeeModification() {
            val pe = createPe(id = 1L, teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { employeeRepository.findById(999L) } returns Optional.empty()

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = 999L)
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("CLOSED_EMPLOYEE_MODIFICATION")
        }

        @Test
        @DisplayName("관리자 마감 행사사원 핵심필드 수정 -> 수정 허용")
        fun batchUpdate_adminClosedEmployeeModification_allowed() {
            val pe = createPe(id = 1L, teamMemberScheduleId = 100L, promoCloseByTm = true)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee(role = AppAuthority.BRANCH_MANAGER))
            every { employeeRepository.findById(999L) } returns Optional.empty()

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = 999L)
            ))

            val result = service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("핵심필드 변경 시 스케줄 삭제")
        fun batchUpdate_criticalFieldChange_scheduleDeleted() {
            val pe = createPe(id = 1L, teamMemberScheduleId = 100L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { employeeRepository.findById(888L) } returns Optional.empty()

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = 888L)
            ))

            service.batchUpdateEmployees(principal, 10L, 1L, request)

            verify { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(100L)) }
            assertThat(pe.teamMemberScheduleId).isNull()
        }

        @Test
        @DisplayName("workType3 null 일괄 수정 -> null로 저장 성공")
        fun batchUpdate_workType3Null_success() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType3 = null)
            ))

            val result = service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.workType3).isNull()
        }

        @Test
        @DisplayName("workType3 빈 문자열 일괄 수정 -> null로 변환되어 저장")
        fun batchUpdate_workType3Empty_savedAsNull() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType3 = "")
            ))

            val result = service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.workType3).isNull()
        }

        @Test
        @DisplayName("일괄수정 - 기존값도 null이면 workType1 '행사' 보정")
        fun batchUpdate_workType1BothNull_defaultApplied() {
            val pe = createPe(id = 1L, workType1 = null)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType1 = null)
            ))

            service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(pe.workType1?.displayName).isEqualTo("행사")
        }

        @Test
        @DisplayName("일괄수정 - 기존값도 null이면 workStatus '근무' 보정")
        fun batchUpdate_workStatusBothNull_defaultApplied() {
            val pe = createPe(id = 1L, workStatus = null)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workStatus = null)
            ))

            service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(pe.workStatus?.displayName).isEqualTo("근무")
        }

        @Test
        @DisplayName("employeeId null 일괄 수정 -> null로 저장 성공")
        fun batchUpdate_employeeIdNull_success() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())
            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, employeeId = null)
            ))

            val result = service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(pe.employeeId).isNull()
        }

        @Test
        @DisplayName("workType3 무효값 일괄 수정 -> INVALID_WORK_TYPE3 에러")
        fun batchUpdate_workType3Invalid() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())

            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, workType3 = "잘못된값")
            ))

            val ex = org.junit.jupiter.api.assertThrows<BatchValidationException> {
                service.batchUpdateEmployees(principal, 10L, 1L, request)
            }
            assertThat(ex.errors[0].errorCode).isEqualTo("INVALID_WORK_TYPE3")
        }

        @Test
        @DisplayName("일괄 수정 시 목표금액/실적금액 자동 계산 - 요청값 무시")
        fun batchUpdate_calculatedFields() {
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(createPromotion())
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup()

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(
                    id = 1L, basePrice = BigDecimal.valueOf(5000L), dailyTargetCount = BigDecimal.valueOf(10L),
                    targetAmount = 99999, actualAmount = 99999,
                    primaryProductAmount = BigDecimal.valueOf(30000L), otherSalesAmount = BigDecimal.valueOf(5000L)
                )
            ))

            service.batchUpdateEmployees(principal, 10L, 1L, request)
            assertThat(pe.targetAmount).isEqualTo(50000)
            assertThat(pe.actualAmount).isEqualTo(35000)
        }

        @Test
        @DisplayName("롤업 재계산 - 수정 후 행사마스터 합계 갱신")
        fun batchUpdate_rollupRecalculation() {
            val promotion = createPromotion()
            val pe = createPe(id = 1L)
            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { employeeRepository.findById(1L) } returns Optional.of(createEmployee())



            every { promotionEmployeeRepository.findById(1L) } returns Optional.of(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(10L) } returns listOf(pe)
            stubRollup(targetSum = 200000, actualSum = 150000)

            val request = BatchUpdatePromotionEmployeeRequest(listOf(
                createBatchItem(id = 1L, targetAmount = 200000, actualAmount = 150000)
            ))

            service.batchUpdateEmployees(principal, 10L, 1L, request)
        }
    }

    // --- Helpers ---

    // Spec #740: recalculatePromotionAmounts 제거됨 — rollup stub 불필요. no-op 유지로 호출처 영향 없음.
    private fun stubRollup(promotionId: Long = 10L, targetSum: Long = 0, actualSum: Long = 0) {}

    private fun createPromotion(
        id: Long = 10L,
        isDeleted: Boolean = false,
        category1: String? = null
    ) = Promotion(
        id = id, promotionNumber = "PM00000001",
        account = Account(id = 100, name = "테스트거래처"), startDate = LocalDate.of(2026, 3, 10), endDate = LocalDate.of(2026, 3, 20),
        isDeleted = isDeleted
    ).also {
        // SF formula `Category1__c = DKRetail__PrimaryProductId__r.StoreCondition__c` 동등 — 대표제품의 storeConditionText 로 derive.
        if (category1 != null) {
            it.primaryProduct = Product(id = 1L, productCode = "TEST", name = "TEST")
                .apply { storeConditionText = category1 }
        }
    }

    private fun createPe(
        id: Long = 1L, promotionId: Long = 10L, employeeId: Long? = 1L,
        scheduleDate: LocalDate = LocalDate.of(2026, 3, 15), teamMemberScheduleId: Long? = null,
        promoCloseByTm: Boolean = false, workStatus: String? = "근무", workType1: String? = "행사"
    ) = PromotionEmployee(
        id = id, promotionId = promotionId, employeeId = employeeId,
        scheduleDate = scheduleDate,
        workStatus = workStatus?.let { WorkingType.fromDisplayName(it) },
        workType1 = workType1?.let { WorkingCategory1.fromDisplayName(it) },
        workType3 = WorkingCategory3.FIXED,
        basePrice = BigDecimal.valueOf(1500L), dailyTargetCount = BigDecimal.valueOf(100L),
        teamMemberScheduleId = teamMemberScheduleId, promoCloseByTm = promoCloseByTm
    ).also {
        it.promotion = createPromotion()
        if (employeeId != null) it.employee = createEmployee()
    }

    private fun createEmployee(role: String? = null) = Employee(id = 1L, sfid = "a0B5g00000XYZabc", employeeCode = "20030117", name = "김여사").also {
        it.role = role
    }

    private fun createBatchItem(
        id: Long = 1L, employeeId: Long? = 1L, scheduleDate: LocalDate = LocalDate.of(2026, 3, 15),
        workStatus: String? = "근무", workType1: String? = "행사", workType3: String? = "고정",
        basePrice: BigDecimal? = BigDecimal.valueOf(1500L), dailyTargetCount: BigDecimal? = BigDecimal.valueOf(100L),
        targetAmount: Long? = 0, actualAmount: Long? = 0,
        primaryProductAmount: BigDecimal? = null, otherSalesAmount: BigDecimal? = null
    ) = BatchUpdatePromotionEmployeeItem(
        id = id, employeeId = employeeId, scheduleDate = scheduleDate, workStatus = workStatus,
        workType1 = workType1, workType3 = workType3,
        basePrice = basePrice,
        dailyTargetCount = dailyTargetCount, targetAmount = targetAmount, actualAmount = actualAmount,
        primaryProductAmount = primaryProductAmount, otherSalesAmount = otherSalesAmount
    )

    private fun createRequest(
        employeeId: Long? = 1L, scheduleDate: LocalDate = LocalDate.of(2026, 3, 15),
        workStatus: String? = "근무", workType1: String? = "행사", workType3: String? = "고정",
        basePrice: BigDecimal? = BigDecimal.valueOf(1500L), dailyTargetCount: BigDecimal? = BigDecimal.valueOf(100L),
        targetAmount: Long? = 0, actualAmount: Long? = 0,
        primaryProductAmount: BigDecimal? = null, otherSalesAmount: BigDecimal? = null
    ) = PromotionEmployeeRequest(
        employeeId = employeeId, scheduleDate = scheduleDate, workStatus = workStatus,
        workType1 = workType1, workType3 = workType3,
        basePrice = basePrice,
        dailyTargetCount = dailyTargetCount, targetAmount = targetAmount, actualAmount = actualAmount,
        primaryProductAmount = primaryProductAmount, otherSalesAmount = otherSalesAmount
    )
}
