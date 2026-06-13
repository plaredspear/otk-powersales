package com.otoki.powersales.promotion.service

import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.enums.PromotionType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.promotion.exception.PromotionForbiddenException
import com.otoki.powersales.promotion.exception.PromotionInvalidParameterException
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@DisplayName("MobilePromotionService 테스트")
class MobilePromotionServiceTest {

    private val promotionRepository: PromotionRepository = mockk()
    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val service = MobilePromotionService(
        promotionRepository,
        promotionEmployeeRepository,
        accountRepository,
        productRepository,
        employeeRepository,
    )

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "USR_SFID_001",
        employeeCode: String = "20030117",
        name: String = "테스트사원",
        status: String? = "재직",
        role: String? = null,
        costCenterCode: String? = "1234"
    ): Employee = Employee(
        id = id,
        sfid = sfid,
        employeeCode = employeeCode,
        name = name,
        status = status,
        role = role,
        costCenterCode = costCenterCode
    )

    private fun createPromotion(
        id: Long = 1L,
        promotionNumber: String = "PM00000001",
        promotionType: PromotionType? = PromotionType.SAMPLING,
        account: Account = createAccount(),
        startDate: LocalDate = LocalDate.of(2026, 3, 1),
        endDate: LocalDate = LocalDate.of(2026, 3, 15),
        costCenterCode: String? = "1234",
        standLocation: StandLocation? = null,
        isClosed: Boolean = false,
        isDeleted: Boolean = false,
        primaryProductId: Long? = null,
        otherProduct: String? = null,
        message: String? = null,
        remark: String? = null,
        productType: String? = null
    ): Promotion = Promotion(
        id = id,
        promotionNumber = promotionNumber,
        promotionType = promotionType,
        account = account,
        startDate = startDate,
        endDate = endDate,
        costCenterCode = costCenterCode,
        standLocation = standLocation,
        isClosed = isClosed,
        isDeleted = isDeleted,
        primaryProductId = primaryProductId,
        otherProduct = otherProduct,
        message = message,
        remark = remark,
        productType = productType
    )

    private fun createPromotionEmployee(
        id: Long = 1L,
        promotionId: Long = 1L,
        employeeId: Long? = 1L,
        scheduleDate: LocalDate? = LocalDate.of(2026, 3, 5),
        workStatus: String? = null,
        workType3: String? = null,
        targetAmount: Long? = 0,
        actualAmount: Long? = 0,
        // SF calculated formula 입력 컬럼 (조원 목표/실적 파생용)
        dailyTargetCount: BigDecimal? = null,
        basePrice: BigDecimal? = null,
        primaryProductAmount: BigDecimal? = null,
        otherSalesAmount: BigDecimal? = null
    ): PromotionEmployee = PromotionEmployee(
        id = id,
        promotionId = promotionId,
        employeeId = employeeId,
        scheduleDate = scheduleDate,
        workStatus = workStatus?.let { WorkingType.fromDisplayName(it) },
        workType3 = workType3?.let { WorkingCategory3.fromDisplayName(it) },
        targetAmount = targetAmount,
        actualAmount = actualAmount,
        dailyTargetCount = dailyTargetCount,
        basePrice = basePrice,
        primaryProductAmount = primaryProductAmount,
        otherSalesAmount = otherSalesAmount
    )

    private fun createAccount(
        id: Long = 100L,
        sfid: String? = "ACC_SFID_001",
        name: String? = "이마트 성수점"
    ): Account = Account(
        id = id,
        sfid = sfid,
        name = name
    )

    private fun createProduct(
        id: Long = 1L,
        name: String? = "진라면"
    ): Product = Product(
        id = id,
        name = name
    )

    @Nested
    @DisplayName("getPromotions - 행사 목록 조회")
    inner class GetPromotionsTests {

        @Test
        @DisplayName("조장 행사 목록 조회 - 동일 지점 행사 반환, myScheduleDate null")
        fun getPromotions_leader_returnsPromotionsWithNullMyScheduleDate() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, account = createAccount(id = 100), promotionType = PromotionType.SAMPLING)
            val account = createAccount(id = 100, name = "이마트 성수점")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(promotion), pageable, 1)

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = null, startDate = null, endDate = null, accountId = null, pageable = any()
                )
            } returns page
            every { accountRepository.findByIdIn(listOf(100)) } returns listOf(account)

            val result = service.getPromotions(userId = 10L, startDate = null, endDate = null, keyword = null, accountId = null, page = 0, size = 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].id).isEqualTo(1L)
            assertThat(result.content[0].accountName).isEqualTo("이마트 성수점")
            assertThat(result.content[0].promotionType).isEqualTo("시식")
            assertThat(result.content[0].myScheduleDate).isNull()
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)

            verify(exactly = 0) { promotionEmployeeRepository.findMinScheduleDateByPromotionIdAndEmployeeId(any(), any()) }
        }

        @Test
        @DisplayName("여사원 행사 목록 조회 - 배정 행사 반환, myScheduleDate 포함")
        fun getPromotions_woman_returnsPromotionsWithMyScheduleDate() {
            val woman = createEmployee(id = 20L, employeeCode = "20030002", role = AppAuthority.WOMAN, costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, account = createAccount(id = 100), promotionType = PromotionType.SAMPLING)
            val account = createAccount(id = 100)
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(promotion), pageable, 1)
            val myScheduleDate = LocalDate.of(2026, 3, 5)

            every { employeeRepository.findById(20L) } returns Optional.of(woman)
            every {
                promotionRepository.searchForMobile(
                    employeeId = 20L, costCenterCode = "1234", isWoman = true,
                    keyword = null, startDate = null, endDate = null, accountId = null, pageable = any()
                )
            } returns page
            every { accountRepository.findByIdIn(listOf(100)) } returns listOf(account)
            every { promotionEmployeeRepository.findMinScheduleDateByPromotionIdAndEmployeeId(1L, 20L) } returns myScheduleDate

            val result = service.getPromotions(userId = 20L, startDate = null, endDate = null, keyword = null, accountId = null, page = 0, size = 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].myScheduleDate).isEqualTo(myScheduleDate)

            verify { promotionEmployeeRepository.findMinScheduleDateByPromotionIdAndEmployeeId(1L, 20L) }
        }

        @Test
        @DisplayName("행사명 파생 - SF formula `productType(대표제품명)` 동등")
        fun getPromotions_derivesPromotionNameFromProductTypeAndPrimaryProduct() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val promotion = createPromotion(
                id = 1L,
                account = createAccount(id = 100),
                promotionType = PromotionType.SAMPLING,
                productType = "냉장/냉동",
                primaryProductId = 5L
            )
            val account = createAccount(id = 100, name = "이마트 성수점")
            val product = createProduct(id = 5L, name = "새우깡")
            val page = PageImpl(listOf(promotion), PageRequest.of(0, 20), 1)

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = null, startDate = null, endDate = null, accountId = null, pageable = any()
                )
            } returns page
            every { accountRepository.findByIdIn(listOf(100)) } returns listOf(account)
            every { productRepository.findAllById(listOf(5L)) } returns listOf(product)

            val result = service.getPromotions(userId = 10L, startDate = null, endDate = null, keyword = null, accountId = null, page = 0, size = 20)

            assertThat(result.content[0].promotionName).isEqualTo("냉장/냉동(새우깡)")
        }

        @Test
        @DisplayName("날짜 필터 - start_date, end_date 전달")
        fun getPromotions_withDateFilter_passesToRepository() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl<Promotion>(emptyList(), pageable, 0)

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = null, startDate = "2026-03-01", endDate = "2026-03-31", accountId = null, pageable = any()
                )
            } returns page

            val result = service.getPromotions(
                userId = 10L,
                startDate = "2026-03-01",
                endDate = "2026-03-31",
                keyword = null,
                accountId = null,
                page = 0,
                size = 20
            )

            assertThat(result.content).isEmpty()
            verify {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = null, startDate = "2026-03-01", endDate = "2026-03-31", accountId = null, pageable = any()
                )
            }
        }

        @Test
        @DisplayName("거래처 필터 - accountId 전달 (레거시 SAPAccountCode 정합)")
        fun getPromotions_withAccountId_passesToRepository() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl<Promotion>(emptyList(), pageable, 0)

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = null, startDate = null, endDate = null, accountId = 100L, pageable = any()
                )
            } returns page

            val result = service.getPromotions(
                userId = 10L,
                startDate = null,
                endDate = null,
                keyword = null,
                accountId = 100L,
                page = 0,
                size = 20
            )

            assertThat(result.content).isEmpty()
            verify {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = null, startDate = null, endDate = null, accountId = 100L, pageable = any()
                )
            }
        }

        @Test
        @DisplayName("키워드 검색 - keyword 전달")
        fun getPromotions_withKeyword_passesToRepository() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl<Promotion>(emptyList(), pageable, 0)

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = "이마트", startDate = null, endDate = null, accountId = null, pageable = any()
                )
            } returns page

            val result = service.getPromotions(
                userId = 10L,
                startDate = null,
                endDate = null,
                keyword = "이마트",
                accountId = null,
                page = 0,
                size = 20
            )

            assertThat(result.content).isEmpty()
            verify {
                promotionRepository.searchForMobile(
                    employeeId = 10L, costCenterCode = "1234", isWoman = false,
                    keyword = "이마트", startDate = null, endDate = null, accountId = null, pageable = any()
                )
            }
        }

        @Test
        @DisplayName("잘못된 날짜 형식 - INVALID_PARAMETER 예외 발생")
        fun getPromotions_invalidDateFormat_throwsInvalidParameter() {
            assertThatThrownBy {
                service.getPromotions(
                    userId = 10L,
                    startDate = "2026/03/01",
                    endDate = null,
                    keyword = null,
                    accountId = null,
                    page = 0,
                    size = 20
                )
            }.isInstanceOf(PromotionInvalidParameterException::class.java)

            verify(exactly = 0) {
                promotionRepository.searchForMobile(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        @Test
        @DisplayName("키워드 초과 - 101자 keyword -> INVALID_PARAMETER 예외 발생")
        fun getPromotions_keywordExceedsLimit_throwsInvalidParameter() {
            val longKeyword = "가".repeat(101)

            assertThatThrownBy {
                service.getPromotions(
                    userId = 10L,
                    startDate = null,
                    endDate = null,
                    keyword = longKeyword,
                    accountId = null,
                    page = 0,
                    size = 20
                )
            }.isInstanceOf(PromotionInvalidParameterException::class.java)

            verify(exactly = 0) {
                promotionRepository.searchForMobile(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("getPromotion - 행사 상세 조회")
    inner class GetPromotionTests {

        @Test
        @DisplayName("조장 행사 상세 조회 - 같은 지점 행사 조회 성공 + 조원 목록 반환")
        fun getPromotion_leader_sameBranch_returnsDetailWithEmployees() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val account = createAccount(id = 100, name = "이마트 성수점")
            val promotion = createPromotion(
                id = 1L,
                costCenterCode = "1234",
                account = account,
                promotionType = PromotionType.SAMPLING,
                primaryProductId = 5L
            )
            val product = createProduct(id = 5L, name = "진라면")
            val employee1 = createPromotionEmployee(
                id = 10L,
                promotionId = 1L,
                employeeId = 20L,
                scheduleDate = LocalDate.of(2026, 3, 5)
            )
            val employee2 = createPromotionEmployee(
                id = 11L,
                promotionId = 1L,
                employeeId = 21L,
                scheduleDate = LocalDate.of(2026, 3, 6)
            )
            val empUser1 = createEmployee(id = 20L, employeeCode = "20030002", name = "김영희")
            val empUser2 = createEmployee(id = 21L, employeeCode = "20030003", name = "이수진")

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every { promotionRepository.findById(1L) } returns Optional.of(promotion)
            every { productRepository.findById(5L) } returns Optional.of(product)
            employee1.employee = empUser1
            employee2.employee = empUser2
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(1L) } returns listOf(employee1, employee2)

            val result = service.getPromotion(userId = 10L, promotionId = 1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.accountName).isEqualTo("이마트 성수점")
            assertThat(result.primaryProductName).isEqualTo("진라면")
            assertThat(result.promotionType).isEqualTo("시식")
            assertThat(result.employees).hasSize(2)
            assertThat(result.employees[0].employeeName).isEqualTo("김영희")
            assertThat(result.employees[1].employeeName).isEqualTo("이수진")

            verify(exactly = 0) { promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(any(), any()) }
        }

        @Test
        @DisplayName("조원 목표/실적은 SF formula(DailyTargetAmount / 총 실적) 로 파생되고 실적 0 은 null 로 표시한다")
        fun getPromotion_derivesEmployeeTargetActualFromFormula() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val account = createAccount(id = 100, name = "이마트 성수점")
            val promotion = createPromotion(id = 1L, costCenterCode = "1234", account = account)
            // 목표: DailyTargetCount(3) × BasePrice(1,000,000) = 3,000,000
            // 실적: PrimaryProductAmount(800,000) + OtherSalesAmount(200,000) = 1,000,000
            val withSales = createPromotionEmployee(
                id = 10L, promotionId = 1L, employeeId = 20L,
                dailyTargetCount = BigDecimal(3),
                basePrice = BigDecimal(1_000_000),
                primaryProductAmount = BigDecimal(800_000),
                otherSalesAmount = BigDecimal(200_000)
            )
            // 실적 입력 없음(총 실적 0) → null, 목표 입력 없음 → null
            val noSales = createPromotionEmployee(
                id = 11L, promotionId = 1L, employeeId = 21L
            )
            val empUser1 = createEmployee(id = 20L, employeeCode = "20030002", name = "김영희")
            val empUser2 = createEmployee(id = 21L, employeeCode = "20030003", name = "이수진")

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every { promotionRepository.findById(1L) } returns Optional.of(promotion)
            withSales.employee = empUser1
            noSales.employee = empUser2
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(1L) } returns listOf(withSales, noSales)

            val result = service.getPromotion(userId = 10L, promotionId = 1L)

            assertThat(result.employees[0].targetAmount).isEqualTo(3_000_000L)
            assertThat(result.employees[0].actualAmount).isEqualTo(1_000_000L)
            assertThat(result.employees[1].targetAmount).isNull()
            assertThat(result.employees[1].actualAmount).isNull()
            // 행사 달성금액 = SF ActualAmount__c Roll-Up SUM 재현 = 전 조원 일별 실적 합(1,000,000 + 0)
            assertThat(result.actualAmount).isEqualTo(1_000_000L)
        }

        @Test
        @DisplayName("여사원 행사 상세 조회 - 배정된 행사 조회 성공")
        fun getPromotion_woman_assigned_returnsDetail() {
            val woman = createEmployee(id = 20L, employeeCode = "20030002", role = AppAuthority.WOMAN, costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, costCenterCode = "1234", account = createAccount(id = 100))
            val employee = createPromotionEmployee(id = 10L, promotionId = 1L, employeeId = 20L)

            every { employeeRepository.findById(20L) } returns Optional.of(woman)
            every { promotionRepository.findById(1L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(1L, 20L) } returns true
            employee.employee = woman
            every { promotionEmployeeRepository.findWithEmployeeByPromotionId(1L) } returns listOf(employee)

            val result = service.getPromotion(userId = 20L, promotionId = 1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.employees).hasSize(1)

            verify { promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(1L, 20L) }
        }

        @Test
        @DisplayName("삭제된 행사 상세 조회 - PROMOTION_NOT_FOUND 예외 발생")
        fun getPromotion_deleted_throwsNotFound() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val deletedPromotion = createPromotion(id = 1L, isDeleted = true)

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every { promotionRepository.findById(1L) } returns Optional.of(deletedPromotion)

            assertThatThrownBy { service.getPromotion(userId = 10L, promotionId = 1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("다른 지점 행사 조회 (조장) - PROMOTION_FORBIDDEN 예외 발생")
        fun getPromotion_leader_differentBranch_throwsForbidden() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, costCenterCode = "5678")

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every { promotionRepository.findById(1L) } returns Optional.of(promotion)

            assertThatThrownBy { service.getPromotion(userId = 10L, promotionId = 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }

        @Test
        @DisplayName("미배정 행사 조회 (여사원) - PROMOTION_FORBIDDEN 예외 발생")
        fun getPromotion_woman_notAssigned_throwsForbidden() {
            val woman = createEmployee(id = 20L, employeeCode = "20030002", role = AppAuthority.WOMAN, costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, costCenterCode = "1234")

            every { employeeRepository.findById(20L) } returns Optional.of(woman)
            every { promotionRepository.findById(1L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(1L, 20L) } returns false

            assertThatThrownBy { service.getPromotion(userId = 20L, promotionId = 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 행사 조회 - PROMOTION_NOT_FOUND 예외 발생")
        fun getPromotion_nonExistent_throwsNotFound() {
            val leader = createEmployee(id = 10L, employeeCode = "20030001", role = AppAuthority.LEADER, costCenterCode = "1234")

            every { employeeRepository.findById(10L) } returns Optional.of(leader)
            every { promotionRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { service.getPromotion(userId = 10L, promotionId = 999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getMyAssignments - 담당 행사 일람 (일 매출 등록 진입)")
    inner class GetMyAssignmentsTests {

        private fun assignmentOf(
            promotion: Promotion,
            id: Long = 1L,
            scheduleDate: LocalDate = LocalDate.of(2026, 3, 5),
            closed: Boolean = false
        ): PromotionEmployee {
            val pe = createPromotionEmployee(
                id = id,
                promotionId = promotion.id,
                employeeId = 20L,
                scheduleDate = scheduleDate
            )
            pe.promotion = promotion
            pe.promoCloseByTm = closed
            return pe
        }

        @Test
        @DisplayName("date 미지정 - 오늘 기준 조회, 행사 정보 매핑")
        fun getMyAssignments_default_today() {
            val woman = createEmployee(id = 20L, role = AppAuthority.WOMAN)
            val account = createAccount(id = 100, name = "이마트 성수점")
            val promotion = createPromotion(id = 1L, account = account, promotionType = PromotionType.SAMPLING, standLocation = StandLocation.END_CAP)
            val today = LocalDate.now()
            val assignment = assignmentOf(promotion, id = 10L, scheduleDate = today)

            every { employeeRepository.findById(20L) } returns Optional.of(woman)
            every { promotionEmployeeRepository.findMyAssignmentsByDate(20L, today) } returns listOf(assignment)
            every { accountRepository.findByIdIn(listOf(100)) } returns listOf(account)

            val result = service.getMyAssignments(userId = 20L, date = null)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(10L)
            assertThat(result[0].promotionId).isEqualTo(1L)
            assertThat(result[0].promotionNumber).isEqualTo("PM00000001")
            assertThat(result[0].accountName).isEqualTo("이마트 성수점")
            assertThat(result[0].promotionType).isEqualTo("시식")
            assertThat(result[0].scheduleDate).isEqualTo(today)
            assertThat(result[0].isClosed).isFalse()

            verify { promotionEmployeeRepository.findMyAssignmentsByDate(20L, today) }
        }

        @Test
        @DisplayName("date 지정 - 해당 일자로 조회, 마감건 isClosed=true")
        fun getMyAssignments_withDate_closedFlag() {
            val woman = createEmployee(id = 20L, role = AppAuthority.WOMAN)
            val account = createAccount(id = 100)
            val promotion = createPromotion(id = 1L, account = account)
            val date = LocalDate.of(2026, 3, 5)
            val assignment = assignmentOf(promotion, id = 10L, scheduleDate = date, closed = true)

            every { employeeRepository.findById(20L) } returns Optional.of(woman)
            every { promotionEmployeeRepository.findMyAssignmentsByDate(20L, date) } returns listOf(assignment)
            every { accountRepository.findByIdIn(listOf(100)) } returns listOf(account)

            val result = service.getMyAssignments(userId = 20L, date = "2026-03-05")

            assertThat(result).hasSize(1)
            assertThat(result[0].isClosed).isTrue()
            verify { promotionEmployeeRepository.findMyAssignmentsByDate(20L, date) }
        }

        @Test
        @DisplayName("담당 행사 없음 - 빈 목록 반환 (account 조회 생략)")
        fun getMyAssignments_empty() {
            val woman = createEmployee(id = 20L, role = AppAuthority.WOMAN)
            val today = LocalDate.now()

            every { employeeRepository.findById(20L) } returns Optional.of(woman)
            every { promotionEmployeeRepository.findMyAssignmentsByDate(20L, today) } returns emptyList()

            val result = service.getMyAssignments(userId = 20L, date = null)

            assertThat(result).isEmpty()
            verify(exactly = 0) { accountRepository.findByIdIn(any()) }
        }

        @Test
        @DisplayName("잘못된 날짜 형식 - INVALID_PARAMETER 예외 발생")
        fun getMyAssignments_invalidDate_throws() {
            assertThatThrownBy { service.getMyAssignments(userId = 20L, date = "2026/03/05") }
                .isInstanceOf(PromotionInvalidParameterException::class.java)

            verify(exactly = 0) { promotionEmployeeRepository.findMyAssignmentsByDate(any(), any()) }
        }
    }
}
