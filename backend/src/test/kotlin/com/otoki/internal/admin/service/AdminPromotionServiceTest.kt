package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionProduct
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionProductRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.ProductRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPromotionService 테스트")
class AdminPromotionServiceTest {

    @Mock private lateinit var promotionRepository: PromotionRepository
    @Mock private lateinit var promotionProductRepository: PromotionProductRepository
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var adminDataScopeService: AdminDataScopeService

    @InjectMocks private lateinit var adminPromotionService: AdminPromotionService

    private val userId = 1L

    @Nested
    @DisplayName("getPromotions - 행사마스터 목록 조회")
    inner class GetPromotionsTests {

        @Test
        @DisplayName("정상 조회 - 전체 권한 사용자 -> 행사마스터 목록 반환")
        fun getPromotions_allBranches_success() {
            // Given
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion()
            val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
            val page = PageImpl(listOf(promotion), pageable, 1)
            whenever(promotionRepository.searchForAdmin(
                keyword = null, promotionType = null, category = null,
                startDate = null, endDate = null, branchCodes = null, pageable = pageable
            )).thenReturn(page)

            val account = createAccount()
            whenever(accountRepository.findByIdIn(listOf(100))).thenReturn(listOf(account))

            val product = createProduct()
            whenever(productRepository.findAllById(listOf(200L))).thenReturn(listOf(product))

            // When
            val result = adminPromotionService.getPromotions(
                userId = userId, keyword = null, promotionType = null, category = null,
                startDate = null, endDate = null, page = 0, size = 20
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].promotionNumber).isEqualTo("PM00000001")
            assertThat(result.content[0].accountName).isEqualTo("GS25 역삼점")
            assertThat(result.content[0].category).isEqualTo("라면")
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("지점 제한 사용자 - branchCodes 비어있음 -> 빈 결과")
        fun getPromotions_emptyBranchCodes_emptyResult() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val result = adminPromotionService.getPromotions(
                userId = userId, keyword = null, promotionType = null, category = null,
                startDate = null, endDate = null, page = 0, size = 20
            )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getPromotion - 행사마스터 상세 조회")
    inner class GetPromotionTests {

        @Test
        @DisplayName("정상 조회 - 유효한 ID -> 상세 정보 반환")
        fun getPromotion_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            val account = createAccount()
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))

            val product = createProduct()
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))

            val result = adminPromotionService.getPromotion(userId, 1L)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
            assertThat(result.accountName).isEqualTo("GS25 역삼점")
            assertThat(result.primaryProductName).isEqualTo("진라면 매운맛 120g")
            assertThat(result.category).isEqualTo("라면")
        }

        @Test
        @DisplayName("음수 ID - id=-1 -> PromotionInvalidParameterException")
        fun getPromotion_negativeId() {
            assertThatThrownBy { adminPromotionService.getPromotion(userId, -1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("미존재 ID - id=999 -> PromotionNotFoundException")
        fun getPromotion_notFound() {
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminPromotionService.getPromotion(userId, 999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사마스터 조회 -> PromotionNotFoundException")
        fun getPromotion_deleted() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.getPromotion(userId, 1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("권한 외 조회 - 지점장이 타 지점 행사 -> PromotionForbiddenException")
        fun getPromotion_forbidden() {
            val scope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion(costCenterCode = "1101")
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.getPromotion(userId, 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("createPromotion - 행사마스터 생성")
    inner class CreatePromotionTests {

        @Test
        @DisplayName("정상 생성 - 필수 필드 모두 포함 -> 201 + promotion_number 자동 생성")
        fun createPromotion_success() {
            val request = createRequest()
            val account = createAccount()
            val product = createProduct()
            val user = createUser(costCenterCode = "1101")

            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(1L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
            assertThat(result.costCenterCode).isEqualTo("1101")
            assertThat(result.category).isEqualTo("라면")
            verify(promotionProductRepository).save(any<PromotionProduct>())
        }

        @Test
        @DisplayName("날짜 범위 오류 - end < start -> InvalidDateRangeException")
        fun createPromotion_invalidDateRange() {
            val request = createRequest(
                startDate = LocalDate.of(2026, 3, 20),
                endDate = LocalDate.of(2026, 3, 10)
            )

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(InvalidDateRangeException::class.java)
        }

        @Test
        @DisplayName("미존재 거래처 - account_id=999 -> AccountNotFoundException")
        fun createPromotion_accountNotFound() {
            val request = createRequest(accountId = 999)
            whenever(accountRepository.findById(999)).thenReturn(Optional.empty())

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(AccountNotFoundException::class.java)
        }

        @Test
        @DisplayName("미존재 상품 - primary_product_id=999 -> ProductNotFoundException")
        fun createPromotion_productNotFound() {
            val request = createRequest(primaryProductId = 999L)
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))
            whenever(productRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(ProductNotFoundException::class.java)
        }

        @Test
        @DisplayName("CC코드 없는 사용자 -> CostCenterNotFoundException")
        fun createPromotion_noCostCenter() {
            val request = createRequest()
            val user = createUser(costCenterCode = null)

            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(CostCenterNotFoundException::class.java)
        }

        @Test
        @DisplayName("대표상품 없이 생성 - primaryProductId=null -> PromotionProduct 미생성")
        fun createPromotion_noPrimaryProduct() {
            val request = createRequest(primaryProductId = null)
            val user = createUser(costCenterCode = "1101")

            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(2L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000002")
            assertThat(result.category).isNull()
            verify(promotionProductRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("updatePromotion - 행사마스터 수정")
    inner class UpdatePromotionTests {

        @Test
        @DisplayName("정상 수정 - 유효한 요청 -> 수정된 데이터 반환")
        fun updatePromotion_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val request = createRequest(promotionName = "수정된 행사명")
            val result = adminPromotionService.updatePromotion(userId, 1L, request)

            assertThat(result.promotionName).isEqualTo("수정된 행사명")
        }

        @Test
        @DisplayName("대표상품 변경 - 기존과 다른 product_id -> PromotionProduct 업데이트")
        fun updatePromotion_changePrimaryProduct() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion(primaryProductId = 200L)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))

            val newProduct = createProduct(id = 300L, name = "새 상품", category1 = "냉동")
            whenever(productRepository.findById(300L)).thenReturn(Optional.of(newProduct))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val existingPP = PromotionProduct(id = 10L, promotionId = 1L, productId = 200L)
            whenever(promotionProductRepository.findByPromotionIdAndIsMainProduct(1L, true)).thenReturn(existingPP)
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val request = createRequest(primaryProductId = 300L)
            val result = adminPromotionService.updatePromotion(userId, 1L, request)

            assertThat(result.category).isEqualTo("냉동")
            verify(promotionProductRepository).save(argThat<PromotionProduct> { productId == 300L })
        }

        @Test
        @DisplayName("삭제된 행사마스터 수정 -> PromotionNotFoundException")
        fun updatePromotion_deleted() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.updatePromotion(userId, 1L, createRequest()) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deletePromotion - 행사마스터 삭제")
    inner class DeletePromotionTests {

        @Test
        @DisplayName("정상 삭제 - 유효한 ID -> soft delete")
        fun deletePromotion_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            adminPromotionService.deletePromotion(userId, 1L)

            assertThat(promotion.isDeleted).isTrue()
            verify(promotionRepository).save(promotion)
        }

        @Test
        @DisplayName("이미 삭제된 행사마스터 -> PromotionNotFoundException")
        fun deletePromotion_alreadyDeleted() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.deletePromotion(userId, 1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("음수 ID -> PromotionInvalidParameterException")
        fun deletePromotion_negativeId() {
            assertThatThrownBy { adminPromotionService.deletePromotion(userId, -1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("권한 외 삭제 - 지점장이 타 지점 행사 -> PromotionForbiddenException")
        fun deletePromotion_forbidden() {
            val scope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            whenever(adminDataScopeService.resolve(userId)).thenReturn(scope)

            val promotion = createPromotion(costCenterCode = "1101")
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.deletePromotion(userId, 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    // Helper methods
    private fun createPromotion(
        id: Long = 1L,
        promotionNumber: String = "PM00000001",
        promotionName: String = "GS25 역삼점 3월 라면 행사",
        accountId: Int = 100,
        primaryProductId: Long? = 200L,
        costCenterCode: String? = "1101",
        isDeleted: Boolean = false
    ) = Promotion(
        id = id,
        promotionNumber = promotionNumber,
        promotionName = promotionName,
        promotionType = "일반행사",
        accountId = accountId,
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = primaryProductId,
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        targetAmount = 5000000,
        costCenterCode = costCenterCode,
        isDeleted = isDeleted
    )

    private fun createAccount(id: Int = 100, name: String = "GS25 역삼점") = Account(
        id = id,
        name = name
    )

    private fun createProduct(
        id: Long = 200L,
        name: String = "진라면 매운맛 120g",
        category1: String = "라면"
    ) = Product(id = id, name = name).also { it.category1 = category1 }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "20030117",
        costCenterCode: String? = "1101"
    ) = User(id = id, employeeId = employeeId, name = "테스트 사용자").also {
        it.costCenterCode = costCenterCode
    }

    private fun createRequest(
        promotionName: String = "GS25 역삼점 3월 라면 행사",
        accountId: Int = 100,
        startDate: LocalDate = LocalDate.of(2026, 3, 10),
        endDate: LocalDate = LocalDate.of(2026, 3, 20),
        primaryProductId: Long? = 200L
    ) = PromotionCreateRequest(
        promotionName = promotionName,
        promotionType = "일반행사",
        accountId = accountId,
        startDate = startDate,
        endDate = endDate,
        primaryProductId = primaryProductId,
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        targetAmount = 5000000
    )
}
