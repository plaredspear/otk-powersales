package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.entity.PromotionProduct
import com.otoki.internal.promotion.entity.PromotionType
import com.otoki.internal.promotion.entity.StandLocation
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionProductRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.promotion.repository.PromotionTypeRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.ProductRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPromotionService 테스트")
class AdminPromotionServiceTest {

    @Mock private lateinit var promotionRepository: PromotionRepository
    @Mock private lateinit var promotionProductRepository: PromotionProductRepository
    @Mock private lateinit var promotionTypeRepository: PromotionTypeRepository
    @Mock private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var dataScopeHolder: DataScopeHolder
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @InjectMocks private lateinit var adminPromotionService: AdminPromotionService

    private val userId = 1L

    @Nested
    @DisplayName("getPromotionFormMeta - 행사마스터 폼 메타 조회")
    inner class GetPromotionFormMetaTests {

        @Test
        @DisplayName("정상 조회 - 활성 행사유형 + 매대위치 Enum 반환")
        fun getPromotionFormMeta_success() {
            val types = listOf(
                createPromotionType(id = 1L, name = "시식", displayOrder = 1),
                createPromotionType(id = 2L, name = "시음", displayOrder = 2)
            )
            whenever(promotionTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(types)

            val result = adminPromotionService.getPromotionFormMeta()

            assertThat(result.promotionTypes).hasSize(2)
            assertThat(result.promotionTypes[0].id).isEqualTo(1L)
            assertThat(result.promotionTypes[0].name).isEqualTo("시식")

            assertThat(result.standLocations).hasSize(6)
            assertThat(result.standLocations[0].value).isEqualTo("FROZEN_EVENT")
            assertThat(result.standLocations[0].name).isEqualTo("냉동행사장")
            assertThat(result.standLocations[5].value).isEqualTo("EVENT_STAND")
            assertThat(result.standLocations[5].name).isEqualTo("행사매대")
        }
    }

    @Nested
    @DisplayName("getPromotions - 행사마스터 목록 조회")
    inner class GetPromotionsTests {

        @Test
        @DisplayName("정상 조회 - 전체 권한 사용자 -> 행사마스터 목록 반환")
        fun getPromotions_allBranches_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion(promotionTypeId = 1L)
            val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
            val page = PageImpl(listOf(promotion), pageable, 1)
            whenever(promotionRepository.searchForAdmin(
                keyword = null, promotionTypeId = null, category = null,
                startDate = null, endDate = null, branchCodes = null, pageable = pageable
            )).thenReturn(page)

            val account = createAccount()
            whenever(accountRepository.findByIdIn(listOf(100))).thenReturn(listOf(account))

            val promotionType = createPromotionType()
            whenever(promotionTypeRepository.findAllById(listOf(1L))).thenReturn(listOf(promotionType))

            val result = adminPromotionService.getPromotions(
                keyword = null, promotionTypeId = null, category = null,
                startDate = null, endDate = null, page = 0, size = 20
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].promotionNumber).isEqualTo("PM00000001")
            assertThat(result.content[0].accountName).isEqualTo("GS25 역삼점")
            assertThat(result.content[0].promotionTypeName).isEqualTo("시식")
            assertThat(result.content[0].category).isEqualTo("라면")
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("지점 제한 사용자 - branchCodes 비어있음 -> 빈 결과")
        fun getPromotions_emptyBranchCodes_emptyResult() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val result = adminPromotionService.getPromotions(
                keyword = null, promotionTypeId = null, category = null,
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
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion(promotionTypeId = 1L)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            val account = createAccount()
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))

            val product = createProduct()
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))

            val promotionType = createPromotionType()
            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))

            val result = adminPromotionService.getPromotion(1L)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
            assertThat(result.accountName).isEqualTo("GS25 역삼점")
            assertThat(result.primaryProductName).isEqualTo("진라면 매운맛 120g")
            assertThat(result.promotionTypeName).isEqualTo("시식")
            assertThat(result.category).isEqualTo("라면")
        }

        @Test
        @DisplayName("음수 ID - id=-1 -> PromotionInvalidParameterException")
        fun getPromotion_negativeId() {
            assertThatThrownBy { adminPromotionService.getPromotion(-1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("미존재 ID - id=999 -> PromotionNotFoundException")
        fun getPromotion_notFound() {
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminPromotionService.getPromotion(999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사마스터 조회 -> PromotionNotFoundException")
        fun getPromotion_deleted() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.getPromotion(1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("권한 외 조회 - 지점장이 타 지점 행사 -> PromotionForbiddenException")
        fun getPromotion_forbidden() {
            val scope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion(costCenterCode = "1101")
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.getPromotion(1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("createPromotion - 행사마스터 생성")
    inner class CreatePromotionTests {

        @Test
        @DisplayName("정상 생성 - 대표제품 지정 -> promotion_name 자동 설정")
        fun createPromotion_success() {
            val request = createRequest(promotionTypeId = 1L)
            val account = createAccount()
            val product = createProduct(name = "꿀배청 680G")
            val user = createUser(costCenterCode = "1101")
            val promotionType = createPromotionType()

            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(1L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
            assertThat(result.promotionName).isEqualTo("꿀배청 680G")
            assertThat(result.costCenterCode).isEqualTo("1101")
            assertThat(result.promotionTypeName).isEqualTo("시식")
            assertThat(result.category).isEqualTo("라면")
            verify(promotionProductRepository).save(any<PromotionProduct>())
        }

        @Test
        @DisplayName("정상 생성 - 거래처 branchName 자동 파생")
        fun createPromotion_branchNameFromAccount() {
            val request = createRequest(promotionTypeId = 1L)
            val account = createAccount(branchName = "서초21지점")
            val product = createProduct(name = "진라면")
            val user = createUser(costCenterCode = "1101")
            val promotionType = createPromotionType()

            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(1L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.branchName).isEqualTo("서초21지점")
        }

        @Test
        @DisplayName("거래처 branchName null -> Promotion branchName도 null")
        fun createPromotion_nullBranchName() {
            val request = createRequest(promotionTypeId = 1L)
            val account = createAccount(branchName = null)
            val product = createProduct(name = "진라면")
            val user = createUser(costCenterCode = "1101")
            val promotionType = createPromotionType()

            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(1L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.branchName).isNull()
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
        @DisplayName("미존재 행사유형 - promotionTypeId=999 -> InvalidPromotionTypeException")
        fun createPromotion_invalidPromotionType() {
            val request = createRequest(promotionTypeId = 999L)
            whenever(promotionTypeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(InvalidPromotionTypeException::class.java)
        }

        @Test
        @DisplayName("비활성 행사유형 -> InvalidPromotionTypeException")
        fun createPromotion_inactivePromotionType() {
            val request = createRequest(promotionTypeId = 5L)
            val inactiveType = createPromotionType(id = 5L, name = "기타", isActive = false)
            whenever(promotionTypeRepository.findById(5L)).thenReturn(Optional.of(inactiveType))

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(InvalidPromotionTypeException::class.java)
        }

        @Test
        @DisplayName("유효하지 않은 매대위치 -> InvalidStandLocationException")
        fun createPromotion_invalidStandLocation() {
            val request = createRequest(standLocation = "없는매대")

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(InvalidStandLocationException::class.java)
        }

        @Test
        @DisplayName("유효한 매대위치 -> 정상 생성")
        fun createPromotion_validStandLocation() {
            val request = createRequest(standLocation = "냉동행사장")
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

            assertThat(result.standLocation).isEqualTo("냉동행사장")
        }

        @Test
        @DisplayName("기타상품에 작은따옴표 포함 -> InvalidOtherProductException")
        fun createPromotion_otherProductWithSingleQuote() {
            val request = createRequest(otherProduct = "참깨라면 '소'")

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(InvalidOtherProductException::class.java)
        }

        @Test
        @DisplayName("기타상품 null -> 정상 생성")
        fun createPromotion_nullOtherProduct() {
            val request = createRequest(otherProduct = null, promotionTypeId = 1L)
            val account = createAccount()
            val product = createProduct()
            val user = createUser(costCenterCode = "1101")
            val promotionType = createPromotionType()

            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(1L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
        }

        @Test
        @DisplayName("기타상품 쌍따옴표 포함 -> 정상 생성")
        fun createPromotion_otherProductWithDoubleQuote() {
            val request = createRequest(otherProduct = "참깨라면 \"대용량\"", promotionTypeId = 1L)
            val account = createAccount()
            val product = createProduct()
            val user = createUser(costCenterCode = "1101")
            val promotionType = createPromotionType()

            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(promotionType))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(account))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(product))
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(promotionRepository.getNextPromotionNumberSeq()).thenReturn(1L)
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
        }

        @Test
        @DisplayName("매대위치 null -> 정상 생성 (nullable)")
        fun createPromotion_nullStandLocation() {
            val request = createRequest(standLocation = null)
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

            assertThat(result.standLocation).isNull()
        }
    }

    @Nested
    @DisplayName("updatePromotion - 행사마스터 수정")
    inner class UpdatePromotionTests {

        @Test
        @DisplayName("정상 수정 - 대표제품 유지 -> promotionName 자동 설정")
        fun updatePromotion_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val request = createRequest()
            val result = adminPromotionService.updatePromotion(1L, userId, request)

            assertThat(result.promotionName).isEqualTo("진라면 매운맛 120g")
        }

        @Test
        @DisplayName("정상 수정 - 거래처 변경 시 branchName 자동 갱신")
        fun updatePromotion_branchNameFromAccount() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            val newAccount = createAccount(id = 200, name = "GS25 강남점", branchName = "서초21지점")
            whenever(accountRepository.findById(200)).thenReturn(Optional.of(newAccount))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(false)
            whenever(promotionEmployeeRepository.findByPromotionId(1L)).thenReturn(emptyList())

            val request = createRequest(accountId = 200)
            val result = adminPromotionService.updatePromotion(1L, userId, request)

            assertThat(result.branchName).isEqualTo("서초21지점")
        }

        @Test
        @DisplayName("대표제품 변경 수정 - 새 제품으로 변경 -> promotionName 갱신")
        fun updatePromotion_changeProduct_promotionNameUpdated() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion(primaryProductId = 200L)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))

            val newProduct = createProduct(id = 300L, name = "진라면 매운맛")
            whenever(productRepository.findById(300L)).thenReturn(Optional.of(newProduct))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val existingPP = PromotionProduct(id = 10L, promotionId = 1L, productId = 200L)
            whenever(promotionProductRepository.findByPromotionIdAndIsMainProduct(1L, true)).thenReturn(existingPP)
            whenever(promotionProductRepository.save(any<PromotionProduct>())).thenAnswer { it.getArgument<PromotionProduct>(0) }

            val request = createRequest(primaryProductId = 300L)
            val result = adminPromotionService.updatePromotion(1L, userId, request)

            assertThat(result.promotionName).isEqualTo("진라면 매운맛")
        }

        @Test
        @DisplayName("대표상품 변경 - 기존과 다른 product_id -> PromotionProduct 업데이트")
        fun updatePromotion_changePrimaryProduct() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

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
            adminPromotionService.updatePromotion(1L, userId, request)

            verify(promotionProductRepository).save(argThat<PromotionProduct> { productId == 300L })
        }

        @Test
        @DisplayName("기타상품에 작은따옴표 포함 -> InvalidOtherProductException")
        fun updatePromotion_otherProductWithSingleQuote() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            val request = createRequest(otherProduct = "라면's")

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, request) }
                .isInstanceOf(InvalidOtherProductException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사마스터 수정 -> PromotionNotFoundException")
        fun updatePromotion_deleted() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, createRequest()) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("비활성 유형으로 수정 -> InvalidPromotionTypeException")
        fun updatePromotion_inactivePromotionType() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            val inactiveType = createPromotionType(id = 5L, name = "기타", isActive = false)
            whenever(promotionTypeRepository.findById(5L)).thenReturn(Optional.of(inactiveType))

            val request = createRequest(promotionTypeId = 5L)

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, request) }
                .isInstanceOf(InvalidPromotionTypeException::class.java)
        }

        @Test
        @DisplayName("USER가 마감 조원 존재 + 거래처 변경 -> ClosedPromotionModificationException")
        fun updatePromotion_closedEmployeeCriticalField() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(true)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))

            val request = createRequest(accountId = 200)

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, request) }
                .isInstanceOf(ClosedPromotionModificationException::class.java)
        }

        @Test
        @DisplayName("USER가 마감 조원 존재 + 날짜 변경 -> ClosedPromotionModificationException")
        fun updatePromotion_closedEmployeeDateChange() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(true)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))

            val request = createRequest(startDate = LocalDate.of(2026, 3, 5))

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, request) }
                .isInstanceOf(ClosedPromotionModificationException::class.java)
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 존재 + 거래처 변경 -> 수정 허용")
        fun updatePromotion_adminClosedEmployeeCriticalField_allowed() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(true)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(appAuthority = "지점장")))

            val newAccount = createAccount(id = 200, name = "GS25 강남점")
            whenever(accountRepository.findById(200)).thenReturn(Optional.of(newAccount))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val pe = PromotionEmployee(
                id = 5L, promotionId = 1L, employeeSfid = "sfid1",
                scheduleDate = LocalDate.of(2026, 3, 15), workStatus = "근무",
                workType1 = "시식", workType3 = "고정", scheduleId = 100L
            )
            whenever(promotionEmployeeRepository.findByPromotionId(1L)).thenReturn(listOf(pe))
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>())).thenAnswer { it.getArgument<PromotionEmployee>(0) }

            val request = createRequest(accountId = 200)
            val result = adminPromotionService.updatePromotion(1L, userId, request)

            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 존재 + 시작일 변경 -> 수정 허용")
        fun updatePromotion_adminClosedEmployeeDateChange_allowed() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(true)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(appAuthority = "지점장")))
            whenever(promotionEmployeeRepository.findMinScheduleDateByPromotionId(1L)).thenReturn(LocalDate.of(2026, 3, 10))
            whenever(promotionEmployeeRepository.findMaxScheduleDateByPromotionId(1L)).thenReturn(LocalDate.of(2026, 3, 18))
            whenever(accountRepository.findById(100)).thenReturn(Optional.of(createAccount()))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            val request = createRequest(startDate = LocalDate.of(2026, 3, 5))
            val result = adminPromotionService.updatePromotion(1L, userId, request)

            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("LEADER가 마감 조원 존재 + 시작일 변경 -> ClosedPromotionModificationException")
        fun updatePromotion_leaderClosedEmployeeDateChange() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(true)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(appAuthority = "조장")))

            val request = createRequest(startDate = LocalDate.of(2026, 3, 5))

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, request) }
                .isInstanceOf(ClosedPromotionModificationException::class.java)
        }

        @Test
        @DisplayName("날짜 축소 시 조원 범위 초과 -> DateRangeConflictException")
        fun updatePromotion_dateRangeConflict() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(false)
            whenever(promotionEmployeeRepository.findMinScheduleDateByPromotionId(1L)).thenReturn(LocalDate.of(2026, 3, 12))
            whenever(promotionEmployeeRepository.findMaxScheduleDateByPromotionId(1L)).thenReturn(LocalDate.of(2026, 3, 18))

            // startDate를 3/13으로 → minDate(3/12) 이후이므로 충돌
            val request = createRequest(startDate = LocalDate.of(2026, 3, 13))

            assertThatThrownBy { adminPromotionService.updatePromotion(1L, userId, request) }
                .isInstanceOf(DateRangeConflictException::class.java)
        }

        @Test
        @DisplayName("거래처 변경 -> 스케줄 초기화")
        fun updatePromotion_accountChange_resetSchedules() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(false)

            val pe = PromotionEmployee(
                id = 5L, promotionId = 1L, employeeSfid = "sfid1",
                scheduleDate = LocalDate.of(2026, 3, 15), workStatus = "근무",
                workType1 = "시식", workType3 = "고정", scheduleId = 100L
            )
            whenever(promotionEmployeeRepository.findByPromotionId(1L)).thenReturn(listOf(pe))

            val newAccount = createAccount(id = 200, name = "GS25 강남점")
            whenever(accountRepository.findById(200)).thenReturn(Optional.of(newAccount))
            whenever(productRepository.findById(200L)).thenReturn(Optional.of(createProduct()))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }
            whenever(promotionEmployeeRepository.save(any<PromotionEmployee>())).thenAnswer { it.getArgument<PromotionEmployee>(0) }

            adminPromotionService.updatePromotion(1L, userId, createRequest(accountId = 200))

            verify(teamMemberScheduleRepository).deleteAllByIdIn(listOf(100L))
            assertThat(pe.scheduleId).isNull()
        }
    }

    @Nested
    @DisplayName("deletePromotion - 행사마스터 삭제")
    inner class DeletePromotionTests {

        @Test
        @DisplayName("정상 삭제 - 유효한 ID -> soft delete + 연쇄 삭제")
        fun deletePromotion_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(false)

            val pe = PromotionEmployee(
                id = 5L, promotionId = 1L, employeeSfid = "sfid1",
                scheduleDate = LocalDate.of(2026, 3, 15), workStatus = "근무",
                workType1 = "시식", workType3 = "고정", scheduleId = 100L
            )
            whenever(promotionEmployeeRepository.findByPromotionId(1L)).thenReturn(listOf(pe))
            whenever(promotionRepository.save(any<Promotion>())).thenAnswer { it.getArgument<Promotion>(0) }

            adminPromotionService.deletePromotion(1L)

            assertThat(promotion.isDeleted).isTrue()
            verify(teamMemberScheduleRepository).deleteAllByIdIn(listOf(100L))
            verify(promotionEmployeeRepository).deleteByPromotionId(1L)
            verify(promotionRepository).save(promotion)
        }

        @Test
        @DisplayName("마감 조원 존재 -> ClosedPromotionDeleteException")
        fun deletePromotion_closedEmployee() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion()
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L)).thenReturn(true)

            assertThatThrownBy { adminPromotionService.deletePromotion(1L) }
                .isInstanceOf(ClosedPromotionDeleteException::class.java)
        }

        @Test
        @DisplayName("이미 삭제된 행사마스터 -> PromotionNotFoundException")
        fun deletePromotion_alreadyDeleted() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.deletePromotion(1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("음수 ID -> PromotionInvalidParameterException")
        fun deletePromotion_negativeId() {
            assertThatThrownBy { adminPromotionService.deletePromotion(-1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("권한 외 삭제 - 지점장이 타 지점 행사 -> PromotionForbiddenException")
        fun deletePromotion_forbidden() {
            val scope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val promotion = createPromotion(costCenterCode = "1101")
            whenever(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { adminPromotionService.deletePromotion(1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    // Helper methods
    private fun createPromotion(
        id: Long = 1L,
        promotionNumber: String = "PM00000001",
        promotionName: String? = "GS25 역삼점 3월 라면 행사",
        promotionTypeId: Long? = null,
        accountId: Int = 100,
        primaryProductId: Long? = 200L,
        costCenterCode: String? = "1101",
        isDeleted: Boolean = false,
        remark: String? = null
    ) = Promotion(
        id = id,
        promotionNumber = promotionNumber,
        promotionName = promotionName,
        promotionTypeId = promotionTypeId,
        accountId = accountId,
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = primaryProductId,
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        costCenterCode = costCenterCode,
        category = "라면",
        isDeleted = isDeleted,
        remark = remark
    )

    private fun createPromotionType(
        id: Long = 1L,
        name: String = "시식",
        displayOrder: Int = 1,
        isActive: Boolean = true
    ) = PromotionType(
        id = id,
        name = name,
        displayOrder = displayOrder,
        isActive = isActive
    )

    private fun createAccount(id: Int = 100, name: String = "GS25 역삼점", branchName: String? = "강남53지점") = Account(
        id = id,
        name = name
    ).also { it.branchName = branchName }

    private fun createProduct(
        id: Long = 200L,
        name: String = "진라면 매운맛 120g",
        category1: String = "라면"
    ) = Product(id = id, name = name).also { it.category1 = category1 }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "20030117",
        costCenterCode: String? = "1101",
        appAuthority: String? = null
    ) = User(id = id, employeeId = employeeId, name = "테스트 사용자").also {
        it.costCenterCode = costCenterCode
        it.appAuthority = appAuthority
    }

    private fun createRequest(
        promotionTypeId: Long? = null,
        accountId: Int = 100,
        startDate: LocalDate = LocalDate.of(2026, 3, 10),
        endDate: LocalDate = LocalDate.of(2026, 3, 20),
        primaryProductId: Long? = 200L,
        standLocation: String? = "냉동행사장",
        otherProduct: String? = "너구리, 진짬뽕",
        remark: String? = null
    ) = PromotionCreateRequest(
        promotionTypeId = promotionTypeId,
        accountId = accountId,
        startDate = startDate,
        endDate = endDate,
        primaryProductId = primaryProductId,
        otherProduct = otherProduct,
        message = "3월 라면 프로모션 진행",
        standLocation = standLocation,
        category = "라면",
        remark = remark
    )
}
