package com.otoki.internal.promotion.service

import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.entity.PromotionType
import com.otoki.internal.promotion.exception.PromotionForbiddenException
import com.otoki.internal.promotion.exception.PromotionInvalidParameterException
import com.otoki.internal.promotion.exception.PromotionNotFoundException
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.promotion.repository.PromotionTypeRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.ProductRepository
import com.otoki.internal.sap.repository.EmployeeRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("MobilePromotionService 테스트")
class MobilePromotionServiceTest {

    @Mock
    private lateinit var promotionRepository: PromotionRepository

    @Mock
    private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository

    @Mock
    private lateinit var promotionTypeRepository: PromotionTypeRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var service: MobilePromotionService

    // --- Helper factories ---

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "USR_SFID_001",
        employeeCode: String = "20030117",
        name: String = "테스트사원",
        status: String? = "재직",
        appAuthority: String? = null,
        costCenterCode: String? = "1234"
    ): Employee = Employee(
        id = id,
        sfid = sfid,
        employeeCode = employeeCode,
        name = name,
        status = status,
        appAuthority = appAuthority,
        costCenterCode = costCenterCode
    )

    private fun createPromotion(
        id: Long = 1L,
        promotionNumber: String = "PM00000001",
        promotionName: String? = "테스트 행사",
        promotionTypeId: Long? = 1L,
        accountId: Int = 100,
        startDate: LocalDate = LocalDate.of(2026, 3, 1),
        endDate: LocalDate = LocalDate.of(2026, 3, 15),
        costCenterCode: String? = "1234",
        standLocation: String? = null,
        category: String? = null,
        targetAmount: Long? = 1000000L,
        actualAmount: Long? = 500000L,
        isClosed: Boolean = false,
        isDeleted: Boolean = false,
        primaryProductId: Long? = null,
        otherProduct: String? = null,
        message: String? = null,
        remark: String? = null
    ): Promotion = Promotion(
        id = id,
        promotionNumber = promotionNumber,
        promotionName = promotionName,
        promotionTypeId = promotionTypeId,
        accountId = accountId,
        startDate = startDate,
        endDate = endDate,
        costCenterCode = costCenterCode,
        standLocation = standLocation,
        category = category,
        targetAmount = targetAmount,
        actualAmount = actualAmount,
        isClosed = isClosed,
        isDeleted = isDeleted,
        primaryProductId = primaryProductId,
        otherProduct = otherProduct,
        message = message,
        remark = remark
    )

    private fun createPromotionEmployee(
        id: Long = 1L,
        promotionId: Long = 1L,
        employeeId: Long? = 1L,
        scheduleDate: LocalDate? = LocalDate.of(2026, 3, 5),
        workStatus: String? = null,
        workType3: String? = null,
        professionalPromotionTeam: String? = null,
        targetAmount: Long? = 0,
        actualAmount: Long? = 0
    ): PromotionEmployee = PromotionEmployee(
        id = id,
        promotionId = promotionId,
        employeeId = employeeId,
        scheduleDate = scheduleDate,
        workStatus = workStatus,
        workType3 = workType3,
        professionalPromotionTeam = professionalPromotionTeam,
        targetAmount = targetAmount,
        actualAmount = actualAmount
    )

    private fun createAccount(
        id: Int = 100,
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

    private fun createPromotionType(
        id: Long = 1L,
        name: String = "시식",
        displayOrder: Int = 1,
        isActive: Boolean = true
    ): PromotionType = PromotionType(
        id = id,
        name = name,
        displayOrder = displayOrder,
        isActive = isActive
    )

    // ========== getPromotions ==========

    @Nested
    @DisplayName("getPromotions - 행사 목록 조회")
    inner class GetPromotionsTests {

        @Test
        @DisplayName("조장 행사 목록 조회 - 동일 지점 행사 반환, myScheduleDate null")
        fun getPromotions_leader_returnsPromotionsWithNullMyScheduleDate() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, accountId = 100, promotionTypeId = 1L)
            val account = createAccount(id = 100, name = "이마트 성수점")
            val promotionType = createPromotionType(id = 1L, name = "시식")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(promotion), pageable, 1)

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.searchForMobile(
                employeeId = eq(10L),
                costCenterCode = eq("1234"),
                isWoman = eq(false),
                keyword = isNull(),
                startDate = isNull(),
                endDate = isNull(),
                pageable = any()
            )).thenReturn(page)
            whenever(accountRepository.findByIdIn(listOf(100))).thenReturn(listOf(account))
            whenever(promotionTypeRepository.findAllById(listOf(1L))).thenReturn(listOf(promotionType))

            // When
            val result = service.getPromotions(userId = 10L, startDate = null, endDate = null, keyword = null, page = 0, size = 20)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].id).isEqualTo(1L)
            assertThat(result.content[0].accountName).isEqualTo("이마트 성수점")
            assertThat(result.content[0].promotionTypeName).isEqualTo("시식")
            assertThat(result.content[0].myScheduleDate).isNull()
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)

            verify(promotionEmployeeRepository, never()).findMinScheduleDateByPromotionIdAndEmployeeId(any(), any())
        }

        @Test
        @DisplayName("여사원 행사 목록 조회 - 배정 행사 반환, myScheduleDate 포함")
        fun getPromotions_woman_returnsPromotionsWithMyScheduleDate() {
            // Given
            val woman = createEmployee(id = 20L, employeeCode = "20030002", appAuthority = "여사원", costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, accountId = 100, promotionTypeId = 1L)
            val account = createAccount(id = 100)
            val promotionType = createPromotionType(id = 1L)
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(promotion), pageable, 1)
            val myScheduleDate = LocalDate.of(2026, 3, 5)

            whenever(employeeRepository.findById(20L)).thenReturn(Optional.of(woman))
            whenever(promotionRepository.searchForMobile(
                employeeId = eq(20L),
                costCenterCode = eq("1234"),
                isWoman = eq(true),
                keyword = isNull(),
                startDate = isNull(),
                endDate = isNull(),
                pageable = any()
            )).thenReturn(page)
            whenever(accountRepository.findByIdIn(listOf(100))).thenReturn(listOf(account))
            whenever(promotionTypeRepository.findAllById(listOf(1L))).thenReturn(listOf(promotionType))
            whenever(promotionEmployeeRepository.findMinScheduleDateByPromotionIdAndEmployeeId(1L, 20L))
                .thenReturn(myScheduleDate)

            // When
            val result = service.getPromotions(userId = 20L, startDate = null, endDate = null, keyword = null, page = 0, size = 20)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].myScheduleDate).isEqualTo(myScheduleDate)

            verify(promotionEmployeeRepository).findMinScheduleDateByPromotionIdAndEmployeeId(1L, 20L)
        }

        @Test
        @DisplayName("날짜 필터 - start_date, end_date 전달")
        fun getPromotions_withDateFilter_passesToRepository() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl<Promotion>(emptyList(), pageable, 0)

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.searchForMobile(
                employeeId = eq(10L),
                costCenterCode = eq("1234"),
                isWoman = eq(false),
                keyword = isNull(),
                startDate = eq("2026-03-01"),
                endDate = eq("2026-03-31"),
                pageable = any()
            )).thenReturn(page)

            // When
            val result = service.getPromotions(
                userId = 10L,
                startDate = "2026-03-01",
                endDate = "2026-03-31",
                keyword = null,
                page = 0,
                size = 20
            )

            // Then
            assertThat(result.content).isEmpty()
            verify(promotionRepository).searchForMobile(
                employeeId = eq(10L),
                costCenterCode = eq("1234"),
                isWoman = eq(false),
                keyword = isNull(),
                startDate = eq("2026-03-01"),
                endDate = eq("2026-03-31"),
                pageable = any()
            )
        }

        @Test
        @DisplayName("키워드 검색 - keyword 전달")
        fun getPromotions_withKeyword_passesToRepository() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl<Promotion>(emptyList(), pageable, 0)

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.searchForMobile(
                employeeId = eq(10L),
                costCenterCode = eq("1234"),
                isWoman = eq(false),
                keyword = eq("이마트"),
                startDate = isNull(),
                endDate = isNull(),
                pageable = any()
            )).thenReturn(page)

            // When
            val result = service.getPromotions(
                userId = 10L,
                startDate = null,
                endDate = null,
                keyword = "이마트",
                page = 0,
                size = 20
            )

            // Then
            assertThat(result.content).isEmpty()
            verify(promotionRepository).searchForMobile(
                employeeId = eq(10L),
                costCenterCode = eq("1234"),
                isWoman = eq(false),
                keyword = eq("이마트"),
                startDate = isNull(),
                endDate = isNull(),
                pageable = any()
            )
        }

        @Test
        @DisplayName("잘못된 날짜 형식 - INVALID_PARAMETER 예외 발생")
        fun getPromotions_invalidDateFormat_throwsInvalidParameter() {
            // Given & When & Then
            assertThatThrownBy {
                service.getPromotions(
                    userId = 10L,
                    startDate = "2026/03/01",
                    endDate = null,
                    keyword = null,
                    page = 0,
                    size = 20
                )
            }.isInstanceOf(PromotionInvalidParameterException::class.java)

            verifyNoInteractions(promotionRepository)
        }

        @Test
        @DisplayName("키워드 초과 - 101자 keyword -> INVALID_PARAMETER 예외 발생")
        fun getPromotions_keywordExceedsLimit_throwsInvalidParameter() {
            // Given
            val longKeyword = "가".repeat(101)

            // When & Then
            assertThatThrownBy {
                service.getPromotions(
                    userId = 10L,
                    startDate = null,
                    endDate = null,
                    keyword = longKeyword,
                    page = 0,
                    size = 20
                )
            }.isInstanceOf(PromotionInvalidParameterException::class.java)

            verifyNoInteractions(promotionRepository)
        }
    }

    // ========== getPromotion ==========

    @Nested
    @DisplayName("getPromotion - 행사 상세 조회")
    inner class GetPromotionTests {

        @Test
        @DisplayName("조장 행사 상세 조회 - 같은 지점 행사 조회 성공 + 조원 목록 반환")
        fun getPromotion_leader_sameBranch_returnsDetailWithEmployees() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")
            val promotion = createPromotion(
                id = 1L,
                costCenterCode = "1234",
                accountId = 100,
                promotionTypeId = 1L,
                primaryProductId = 5L
            )
            val account = createAccount(id = 100, name = "이마트 성수점")
            val product = createProduct(id = 5L, name = "진라면")
            val promotionType = createPromotionType(id = 1L, name = "시식")
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

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(5L)).thenReturn(Optional.of(product))
            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(1L))
                .thenReturn(listOf(employee1, employee2))
            whenever(employeeRepository.findAllById(listOf(20L, 21L)))
                .thenReturn(listOf(empUser1, empUser2))

            // When
            val result = service.getPromotion(userId = 10L, promotionId = 1L)

            // Then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.accountName).isEqualTo("이마트 성수점")
            assertThat(result.primaryProductName).isEqualTo("진라면")
            assertThat(result.promotionTypeName).isEqualTo("시식")
            assertThat(result.employees).hasSize(2)
            assertThat(result.employees[0].employeeName).isEqualTo("김영희")
            assertThat(result.employees[1].employeeName).isEqualTo("이수진")

            verify(promotionEmployeeRepository, never()).existsByPromotionIdAndEmployeeId(any(), any())
        }

        @Test
        @DisplayName("여사원 행사 상세 조회 - 배정된 행사 조회 성공")
        fun getPromotion_woman_assigned_returnsDetail() {
            // Given
            val woman = createEmployee(id = 20L, employeeCode = "20030002", appAuthority = "여사원", costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, costCenterCode = "1234", accountId = 100)
            val account = createAccount(id = 100)
            val employee = createPromotionEmployee(id = 10L, promotionId = 1L, employeeId = 20L)

            whenever(employeeRepository.findById(20L)).thenReturn(Optional.of(woman))
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(1L, 20L)).thenReturn(true)
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(1L))
                .thenReturn(listOf(employee))
            whenever(employeeRepository.findAllById(listOf(20L)))
                .thenReturn(listOf(woman))

            // When
            val result = service.getPromotion(userId = 20L, promotionId = 1L)

            // Then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.employees).hasSize(1)

            verify(promotionEmployeeRepository).existsByPromotionIdAndEmployeeId(1L, 20L)
        }

        @Test
        @DisplayName("삭제된 행사 상세 조회 - PROMOTION_NOT_FOUND 예외 발생")
        fun getPromotion_deleted_throwsNotFound() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")
            val deletedPromotion = createPromotion(id = 1L, isDeleted = true)

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(deletedPromotion))

            // When & Then
            assertThatThrownBy { service.getPromotion(userId = 10L, promotionId = 1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("다른 지점 행사 조회 (조장) - PROMOTION_FORBIDDEN 예외 발생")
        fun getPromotion_leader_differentBranch_throwsForbidden() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, costCenterCode = "5678")

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            // When & Then
            assertThatThrownBy { service.getPromotion(userId = 10L, promotionId = 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }

        @Test
        @DisplayName("미배정 행사 조회 (여사원) - PROMOTION_FORBIDDEN 예외 발생")
        fun getPromotion_woman_notAssigned_throwsForbidden() {
            // Given
            val woman = createEmployee(id = 20L, employeeCode = "20030002", appAuthority = "여사원", costCenterCode = "1234")
            val promotion = createPromotion(id = 1L, costCenterCode = "1234")

            whenever(employeeRepository.findById(20L)).thenReturn(Optional.of(woman))
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(1L, 20L)).thenReturn(false)

            // When & Then
            assertThatThrownBy { service.getPromotion(userId = 20L, promotionId = 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 행사 조회 - PROMOTION_NOT_FOUND 예외 발생")
        fun getPromotion_nonExistent_throwsNotFound() {
            // Given
            val leader = createEmployee(id = 10L, employeeCode = "20030001", appAuthority = "조장", costCenterCode = "1234")

            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(leader))
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { service.getPromotion(userId = 10L, promotionId = 999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }
}
