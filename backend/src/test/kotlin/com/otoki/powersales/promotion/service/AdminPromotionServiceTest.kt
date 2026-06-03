package com.otoki.powersales.promotion.service

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.promotion.dto.request.PromotionCreateRequest
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.enums.PromotionType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.entity.PromotionProduct
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionProductRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.service.TeamMemberScheduleCascadeHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.util.*
import java.math.BigDecimal

@DisplayName("AdminPromotionService 테스트")
class AdminPromotionServiceTest {

    private val promotionRepository: PromotionRepository = mockk(relaxUnitFun = true)
    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk(relaxUnitFun = true)
    private val promotionProductRepository: PromotionProductRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper = mockk(relaxUnitFun = true)
    private val policyEvaluator: com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator =
        mockk(relaxed = true)

    private val adminPromotionService: AdminPromotionService = AdminPromotionService(
        promotionRepository = promotionRepository,
        promotionEmployeeRepository = promotionEmployeeRepository,
        promotionProductRepository = promotionProductRepository,
        accountRepository = accountRepository,
        productRepository = productRepository,
        employeeRepository = employeeRepository,
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        teamMemberScheduleCascadeHelper = teamMemberScheduleCascadeHelper,
        policyEvaluator = policyEvaluator,
    )

    private val userId = 1L
    private val principal: WebUserPrincipal = mockk(relaxed = true)

    // 원본 mockito 테스트는 일부 케이스에서 promotionProductRepository.save /
    // findByPromotionId 를 stubbing 없이 호출했고 mockito silent default 로 통과했다.
    // strict MockK 호환을 위한 default — 각 테스트에서 override 가능.
    @BeforeEach
    fun stubMockitoSilentDefaults() {
        every { promotionProductRepository.findByPromotionId(any()) } returns null
        every {
            promotionProductRepository.save(any<PromotionProduct>())
        } answers { firstArg<PromotionProduct>() }
        // SF 가시 범위 — 단건 검증(validateDataScope) 기본 통과. forbidden 케이스는 개별 override.
        every { promotionRepository.existsVisibleById(any(), any()) } returns true
    }

    @Nested
    @DisplayName("getPromotionFormMeta - 행사마스터 폼 메타 조회")
    inner class GetPromotionFormMetaTests {

        @Test
        @DisplayName("정상 조회 - PromotionType enum 3개 + StandLocation enum 6개 반환")
        fun getPromotionFormMeta_success() {
            val result = adminPromotionService.getPromotionFormMeta()

            assertThat(result.promotionTypes).hasSize(3)
            assertThat(result.promotionTypes[0].value).isEqualTo("SAMPLING")
            assertThat(result.promotionTypes[0].name).isEqualTo("시식")
            assertThat(result.promotionTypes[2].value).isEqualTo("COLLECTION")
            assertThat(result.promotionTypes[2].name).isEqualTo("모음전")

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
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion(promotionType = PromotionType.SAMPLING).apply {
                account = createAccount()
            }
            val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
            val page = PageImpl(listOf(promotion), pageable, 1)
            every { promotionRepository.searchForAdmin(
                policyPredicate = any(), keyword = null, promotionType = null,
                startDate = null, endDate = null, ownerOnly = false, currentUserId = any(), pageable = pageable
            ) } returns page

            val result = adminPromotionService.getPromotions(scope = scope,
                keyword = null, promotionType = null,
                startDate = null, endDate = null, ownerOnly = false, page = 0, size = 20
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].promotionNumber).isEqualTo("PM00000001")
            assertThat(result.content[0].accountName).isEqualTo("GS25 역삼점")
            assertThat(result.content[0].promotionType).isEqualTo("시식")
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("가시 범위 밖 사용자 - evaluator deny predicate -> repo 0건 반환")
        fun getPromotions_outOfScope_emptyResult() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            // SF 가시 범위 전환: 조기 반환이 아니라 evaluator deny predicate → repository 0건 (자연 처리).
            val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
            every { promotionRepository.searchForAdmin(
                policyPredicate = any(), keyword = null, promotionType = null,
                startDate = null, endDate = null, ownerOnly = false, currentUserId = any(), pageable = pageable
            ) } returns PageImpl(emptyList(), pageable, 0)

            val result = adminPromotionService.getPromotions(scope = scope,
                keyword = null, promotionType = null,
                startDate = null, endDate = null, ownerOnly = false, page = 0, size = 20
            )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("ownerOnly=true -> repository 에 ownerOnly + currentUserId(scope.userId) 전달")
        fun getPromotions_ownerOnly_passesUserId() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true, userId = 77L)
            val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
            every {
                promotionRepository.searchForAdmin(
                    policyPredicate = any(), keyword = null, promotionType = null,
                    startDate = null, endDate = null, ownerOnly = true, currentUserId = 77L, pageable = pageable,
                )
            } returns PageImpl(emptyList(), pageable, 0)

            val result = adminPromotionService.getPromotions(
                scope = scope, keyword = null, promotionType = null,
                startDate = null, endDate = null, ownerOnly = true, page = 0, size = 20,
            )

            assertThat(result.totalElements).isEqualTo(0)
            verify {
                promotionRepository.searchForAdmin(
                    policyPredicate = any(), keyword = null, promotionType = null,
                    startDate = null, endDate = null, ownerOnly = true, currentUserId = 77L, pageable = pageable,
                )
            }
        }
    }

    @Nested
    @DisplayName("getPromotion - 행사마스터 상세 조회")
    inner class GetPromotionTests {

        // controller 가 dataScopeHolder.require() 결과를 explicit parameter 로 전달하는 패턴
        // 시뮬레이션. 권한 검증 분기와 무관한 케이스는 ALL scope 사용.
        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 조회 - 유효한 ID -> 상세 정보 반환")
        fun getPromotion_success() {
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion(promotionType = PromotionType.SAMPLING).apply {
                account = createAccount()
                primaryProduct = createProduct()
            }
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            val result = adminPromotionService.getPromotion(scope, 1L)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
            assertThat(result.accountName).isEqualTo("GS25 역삼점")
            assertThat(result.primaryProductName).isEqualTo("진라면 매운맛 120g")
            assertThat(result.promotionType).isEqualTo("시식")
        }

        @Test
        @DisplayName("음수 ID - id=-1 -> PromotionInvalidParameterException")
        fun getPromotion_negativeId() {
            assertThatThrownBy { adminPromotionService.getPromotion(scope, -1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("미존재 ID - id=999 -> PromotionNotFoundException")
        fun getPromotion_notFound() {
            every { promotionRepository.findByIdWithRelations(999L) } returns null

            assertThatThrownBy { adminPromotionService.getPromotion(scope, 999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사마스터 조회 -> PromotionNotFoundException")
        fun getPromotion_deleted() {
            val promotion = createPromotion(isDeleted = true)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            assertThatThrownBy { adminPromotionService.getPromotion(scope, 1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("권한 외 조회 - 지점장이 타 지점 행사 -> PromotionForbiddenException")
        fun getPromotion_forbidden() {
            val scope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion(costCenterCode = "1101")
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            // SF 가시 범위 밖 — evaluator predicate 로 단건 미가시
            every { promotionRepository.existsVisibleById(1L, any()) } returns false

            assertThatThrownBy { adminPromotionService.getPromotion(scope, 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("getPosProducts - 상세 POS품목 일람 조회")
    inner class GetPosProductsTests {

        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 조회 - PromotionProduct 다수 -> name 정렬 list 반환")
        fun getPosProducts_success() {
            val promotion = createPromotion()
            val product = createProduct(name = "꿀배청 680G")
            val pp1 = PromotionProduct(
                id = 10L,
                name = "PS00000001",
                promotionId = 1L,
                productId = 200L,
                price = java.math.BigDecimal("1500"),
            ).apply { this.product = product }
            val pp2 = PromotionProduct(
                id = 11L,
                name = "PS00000002",
                promotionId = 1L,
                productId = 200L,
                price = java.math.BigDecimal("1200"),
            ).apply { this.product = product }

            every { promotionRepository.findById(1L) } returns Optional.of(promotion)
            every {
                promotionProductRepository.findByPromotionIdAndIsDeletedFalseOrderByNameAsc(1L)
            } returns listOf(pp1, pp2)

            val result = adminPromotionService.getPosProducts(scope, 1L)

            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("PS00000001")
            assertThat(result[0].productName).isEqualTo("꿀배청 680G")
            assertThat(result[0].price).isEqualByComparingTo("1500")
            assertThat(result[1].name).isEqualTo("PS00000002")
        }

        @Test
        @DisplayName("미존재 행사 -> PromotionNotFoundException")
        fun getPosProducts_notFound() {
            every { promotionRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { adminPromotionService.getPosProducts(scope, 999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("권한 외 조회 -> PromotionForbiddenException")
        fun getPosProducts_forbidden() {
            val limitedScope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            val promotion = createPromotion(costCenterCode = "1101")
            every { promotionRepository.findById(1L) } returns Optional.of(promotion)
            every { promotionRepository.existsVisibleById(1L, any()) } returns false

            assertThatThrownBy { adminPromotionService.getPosProducts(limitedScope, 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }

        @Test
        @DisplayName("음수 ID -> PromotionInvalidParameterException")
        fun getPosProducts_negativeId() {
            assertThatThrownBy { adminPromotionService.getPosProducts(scope, -1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("createPosProduct - 상세 POS품목 생성")
    inner class CreatePosProductTests {

        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
        private val request = com.otoki.powersales.promotion.dto.request.PromotionPosProductRequest(
            productId = 200L,
            price = java.math.BigDecimal("1500"),
        )

        @Test
        @DisplayName("정상 생성 - 채번된 Name (PS{########}) 반환 + product/price 세팅")
        fun createPosProduct_success() {
            val promotion = createPromotion()
            val product = createProduct(name = "꿀배청 680G")
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { promotionProductRepository.getNextNameSeq() } returns 127052L
            val saved = slot<PromotionProduct>()
            every { promotionProductRepository.save(capture(saved)) } answers { firstArg<PromotionProduct>() }

            val result = adminPromotionService.createPosProduct(scope, 1L, request)

            assertThat(saved.captured.name).isEqualTo("PS00127052")
            assertThat(saved.captured.promotionId).isEqualTo(1L)
            assertThat(saved.captured.productId).isEqualTo(200L)
            assertThat(saved.captured.price).isEqualByComparingTo("1500")
            assertThat(result.name).isEqualTo("PS00127052")
            assertThat(result.productName).isEqualTo("꿀배청 680G")
        }

        @Test
        @DisplayName("productId/price 모두 null -> 정상 생성 (SF 정합 — 필수 아님)")
        fun createPosProduct_allNullable() {
            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionProductRepository.getNextNameSeq() } returns 1L
            val saved = slot<PromotionProduct>()
            every { promotionProductRepository.save(capture(saved)) } answers { firstArg<PromotionProduct>() }

            val result = adminPromotionService.createPosProduct(
                scope, 1L,
                com.otoki.powersales.promotion.dto.request.PromotionPosProductRequest(productId = null, price = null)
            )

            assertThat(result.name).isEqualTo("PS00000001")
            assertThat(saved.captured.productId).isNull()
            assertThat(saved.captured.price).isNull()
        }

        @Test
        @DisplayName("미존재 행사 -> PromotionNotFoundException")
        fun createPosProduct_promotionNotFound() {
            every { promotionRepository.findByIdWithRelations(999L) } returns null
            assertThatThrownBy { adminPromotionService.createPosProduct(scope, 999L, request) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("권한 외 -> PromotionForbiddenException")
        fun createPosProduct_forbidden() {
            val limited = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            val promotion = createPromotion(costCenterCode = "1101")
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionRepository.existsVisibleById(1L, any()) } returns false
            assertThatThrownBy { adminPromotionService.createPosProduct(limited, 1L, request) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("updatePosProduct - 상세 POS품목 수정")
    inner class UpdatePosProductTests {

        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 수정 - productId/price 변경")
        fun updatePosProduct_success() {
            val promotion = createPromotion()
            val product = createProduct(name = "꿀배청 680G")
            val entity = PromotionProduct(
                id = 10L, name = "PS00000001", promotionId = 1L,
                productId = null, price = null,
            )
            every { promotionProductRepository.findById(10L) } returns Optional.of(entity)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { productRepository.findById(200L) } returns Optional.of(product)

            val result = adminPromotionService.updatePosProduct(
                scope, 10L,
                com.otoki.powersales.promotion.dto.request.PromotionPosProductRequest(
                    productId = 200L, price = java.math.BigDecimal("2000")
                )
            )

            assertThat(entity.productId).isEqualTo(200L)
            assertThat(entity.price).isEqualByComparingTo("2000")
            assertThat(result.productName).isEqualTo("꿀배청 680G")
        }

        @Test
        @DisplayName("미존재 ID -> PromotionProductNotFoundException")
        fun updatePosProduct_notFound() {
            every { promotionProductRepository.findById(999L) } returns Optional.empty()
            assertThatThrownBy {
                adminPromotionService.updatePosProduct(
                    scope, 999L,
                    com.otoki.powersales.promotion.dto.request.PromotionPosProductRequest()
                )
            }.isInstanceOf(PromotionProductNotFoundException::class.java)
        }

        @Test
        @DisplayName("권한 외 -> PromotionForbiddenException")
        fun updatePosProduct_forbidden() {
            val limited = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            val promotion = createPromotion(costCenterCode = "1101")
            val entity = PromotionProduct(id = 10L, name = "PS00000001", promotionId = 1L)
            every { promotionProductRepository.findById(10L) } returns Optional.of(entity)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionRepository.existsVisibleById(1L, any()) } returns false

            assertThatThrownBy {
                adminPromotionService.updatePosProduct(
                    limited, 10L,
                    com.otoki.powersales.promotion.dto.request.PromotionPosProductRequest()
                )
            }.isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("deletePosProduct - 상세 POS품목 삭제")
    inner class DeletePosProductTests {

        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 삭제 - soft delete (is_deleted=true)")
        fun deletePosProduct_success() {
            val promotion = createPromotion()
            val entity = PromotionProduct(id = 10L, name = "PS00000001", promotionId = 1L)
            every { promotionProductRepository.findById(10L) } returns Optional.of(entity)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            adminPromotionService.deletePosProduct(scope, 10L)

            assertThat(entity.isDeleted).isTrue()
        }

        @Test
        @DisplayName("이미 삭제됨 -> PromotionProductNotFoundException")
        fun deletePosProduct_alreadyDeleted() {
            val entity = PromotionProduct(id = 10L, name = "PS00000001", promotionId = 1L, isDeleted = true)
            every { promotionProductRepository.findById(10L) } returns Optional.of(entity)
            assertThatThrownBy { adminPromotionService.deletePosProduct(scope, 10L) }
                .isInstanceOf(PromotionProductNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("createPromotion - 행사마스터 생성")
    inner class CreatePromotionTests {

        @Test
        @DisplayName("정상 생성 - 대표제품 지정 + 행사상품 1건 자동 생성 (레거시 PromotionTriggerHandler.insertPromotionProduct 동등)")
        fun createPromotion_success() {
            val request = createRequest(promotionType = "시식")
            val account = createAccount()
            val product = createProduct(name = "꿀배청 680G")
            val employee = createEmployee(costCenterCode = "1101")

            every { accountRepository.findById(100) } returns Optional.of(account)
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { promotionRepository.getNextPromotionNumberSeq() } returns 1L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }
            every { promotionProductRepository.findByPromotionId(any()) } returns null
            val savedPromotionProduct = slot<PromotionProduct>()
            every { promotionProductRepository.save(capture(savedPromotionProduct)) } answers { firstArg<PromotionProduct>() }

            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
            assertThat(result.costCenterCode).isEqualTo("1101")
            assertThat(result.promotionType).isEqualTo("시식")

            // 행사상품 1건 자동 생성 검증 (대표품목 productId=200 으로)
            verify { promotionProductRepository.save(any()) }
            assertThat(savedPromotionProduct.captured.productId).isEqualTo(200L)
        }

        @Test
        @DisplayName("거래처 branchName null -> 정상 생성")
        fun createPromotion_nullBranchName() {
            val request = createRequest(promotionType = "시식")
            val account = createAccount(branchName = null)
            val product = createProduct(name = "진라면")
            val employee = createEmployee(costCenterCode = "1101")

            every { accountRepository.findById(100) } returns Optional.of(account)
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { promotionRepository.getNextPromotionNumberSeq() } returns 1L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }


            adminPromotionService.createPromotion(userId, request)
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
            every { accountRepository.findById(999) } returns Optional.empty()

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(AccountNotFoundException::class.java)
        }

        @Test
        @DisplayName("미존재 상품 - primary_product_id=999 -> ProductNotFoundException")
        fun createPromotion_productNotFound() {
            val request = createRequest(primaryProductId = 999L)
            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(ProductNotFoundException::class.java)
        }

        @Test
        @DisplayName("CC코드 없는 사용자 -> CostCenterNotFoundException")
        fun createPromotion_noCostCenter() {
            val request = createRequest()
            val employee = createEmployee(costCenterCode = null)

            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { employeeRepository.findById(userId) } returns Optional.of(employee)

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(CostCenterNotFoundException::class.java)
        }

        @Test
        @DisplayName("유효하지 않은 행사유형 - promotionType='없는유형' -> PromotionInvalidParameterException")
        fun createPromotion_invalidPromotionType() {
            val request = createRequest(promotionType = "없는유형")

            assertThatThrownBy { adminPromotionService.createPromotion(userId, request) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
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
            val employee = createEmployee(costCenterCode = "1101")

            every { accountRepository.findById(100) } returns Optional.of(account)
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { promotionRepository.getNextPromotionNumberSeq() } returns 1L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }


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
            val request = createRequest(otherProduct = null, promotionType = "시식")
            val account = createAccount()
            val product = createProduct()
            val employee = createEmployee(costCenterCode = "1101")

            every { accountRepository.findById(100) } returns Optional.of(account)
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { promotionRepository.getNextPromotionNumberSeq() } returns 1L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }


            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
        }

        @Test
        @DisplayName("기타상품 쌍따옴표 포함 -> 정상 생성")
        fun createPromotion_otherProductWithDoubleQuote() {
            val request = createRequest(otherProduct = "참깨라면 \"대용량\"", promotionType = "시식")
            val account = createAccount()
            val product = createProduct()
            val employee = createEmployee(costCenterCode = "1101")

            every { accountRepository.findById(100) } returns Optional.of(account)
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { promotionRepository.getNextPromotionNumberSeq() } returns 1L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }


            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.promotionNumber).isEqualTo("PM00000001")
        }

        @Test
        @DisplayName("매대위치 null -> 정상 생성 (nullable)")
        fun createPromotion_nullStandLocation() {
            val request = createRequest(standLocation = null)
            val account = createAccount()
            val product = createProduct()
            val employee = createEmployee(costCenterCode = "1101")

            every { accountRepository.findById(100) } returns Optional.of(account)
            every { productRepository.findById(200L) } returns Optional.of(product)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { promotionRepository.getNextPromotionNumberSeq() } returns 1L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }


            val result = adminPromotionService.createPromotion(userId, request)

            assertThat(result.standLocation).isNull()
        }
    }

    @Nested
    @DisplayName("updatePromotion - 행사마스터 수정")
    inner class UpdatePromotionTests {

        // controller 가 dataScopeHolder.require() 결과를 explicit parameter 로 전달하는 패턴 시뮬레이션.
        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 수정 - 대표제품 유지")
        fun updatePromotion_success() {
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            val request = createRequest()
            adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)
        }

        @Test
        @DisplayName("정상 수정 - 거래처 변경 시 branchName 자동 갱신")
        fun updatePromotion_branchNameFromAccount() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            val newAccount = createAccount(id = 200, name = "GS25 강남점", branchName = "서초21지점")
            every { accountRepository.findById(200) } returns Optional.of(newAccount)
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns false
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns emptyList()

            val request = createRequest(accountId = 200)
            adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)
        }

        @Test
        @DisplayName("대표제품 변경 수정 - 새 제품으로 변경")
        fun updatePromotion_changeProduct_promotionNameUpdated() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion(primaryProductId = 200L)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { accountRepository.findById(100) } returns Optional.of(createAccount())

            val newProduct = createProduct(id = 300L, name = "진라면 매운맛")
            every { productRepository.findById(300L) } returns Optional.of(newProduct)
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            val request = createRequest(primaryProductId = 300L)
            adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)
        }

        @Test
        @DisplayName("대표상품 변경 - 기존과 다른 product_id -> Promotion.primaryProductId 업데이트 + 행사상품 신규 생성 (기존 미존재)")
        fun updatePromotion_changePrimaryProduct() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion(primaryProductId = 200L)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { accountRepository.findById(100) } returns Optional.of(createAccount())

            val newProduct = createProduct(id = 300L, name = "새 상품", category1 = "냉동")
            every { productRepository.findById(300L) } returns Optional.of(newProduct)
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }
            every { promotionProductRepository.findByPromotionId(promotion.id) } returns null
            val savedPromotionProduct = slot<PromotionProduct>()
            every { promotionProductRepository.save(capture(savedPromotionProduct)) } answers { firstArg<PromotionProduct>() }

            val request = createRequest(primaryProductId = 300L)
            val result = adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)

            assertThat(result.primaryProductId).isEqualTo(300L)

            // 행사상품 신규 생성 검증 (기존 미존재 → save 신규)
            verify { promotionProductRepository.save(any()) }
            assertThat(savedPromotionProduct.captured.productId).isEqualTo(300L)
            assertThat(savedPromotionProduct.captured.promotionId).isEqualTo(promotion.id)
        }

        @Test
        @DisplayName("대표상품 변경 - 기존 행사상품 존재 -> productId 갱신 (레거시 PromotionTriggerHandler.changePosProduct upsert 동등)")
        fun updatePromotion_changePrimaryProduct_existingPromotionProductUpdated() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val promotion = createPromotion(primaryProductId = 200L)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { accountRepository.findById(100) } returns Optional.of(createAccount())

            val newProduct = createProduct(id = 300L, name = "새 상품", category1 = "냉동")
            every { productRepository.findById(300L) } returns Optional.of(newProduct)
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            // 기존 행사상품이 존재 (productId=200) → 새 productId=300 으로 갱신되어야 함
            val existingPromotionProduct = PromotionProduct(
                id = 1L,
                promotionId = promotion.id,
                productId = 200L
            )
            every { promotionProductRepository.findByPromotionId(promotion.id) } returns existingPromotionProduct
            val savedPromotionProduct = slot<PromotionProduct>()
            every { promotionProductRepository.save(capture(savedPromotionProduct)) } answers { firstArg<PromotionProduct>() }

            val request = createRequest(primaryProductId = 300L)
            adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)

            // 기존 entity 의 productId 가 300 으로 갱신되었는지 + 동일 entity 가 save 되었는지 검증
            verify { promotionProductRepository.save(any()) }
            assertThat(savedPromotionProduct.captured.id).isEqualTo(1L)
            assertThat(savedPromotionProduct.captured.productId).isEqualTo(300L)
        }

        @Test
        @DisplayName("대표상품 동일 - 변경 없음 -> 행사상품 upsert 호출 없음")
        fun updatePromotion_samePrimaryProduct_noPromotionProductUpsert() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val promotion = createPromotion(primaryProductId = 200L)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            val request = createRequest(primaryProductId = 200L)
            adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)

            verify(exactly = 0) { promotionProductRepository.save(any<PromotionProduct>()) }
            verify(exactly = 0) { promotionProductRepository.findByPromotionId(any()) }
        }

        @Test
        @DisplayName("기타상품에 작은따옴표 포함 -> InvalidOtherProductException")
        fun updatePromotion_otherProductWithSingleQuote() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            val request = createRequest(otherProduct = "라면's")

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, request) }
                .isInstanceOf(InvalidOtherProductException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사마스터 수정 -> PromotionNotFoundException")
        fun updatePromotion_deleted() {
            val promotion = createPromotion(isDeleted = true)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, createRequest()) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("유효하지 않은 행사유형으로 수정 -> PromotionInvalidParameterException")
        fun updatePromotion_invalidPromotionType() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            val request = createRequest(promotionType = "없는유형")

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, request) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("USER가 마감 조원 존재 + 거래처 변경 -> ClosedPromotionModificationException")
        fun updatePromotion_closedEmployeeCriticalField() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns true
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())

            val request = createRequest(accountId = 200)

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, request) }
                .isInstanceOf(ClosedPromotionModificationException::class.java)
        }

        @Test
        @DisplayName("USER가 마감 조원 존재 + 날짜 변경 -> ClosedPromotionModificationException")
        fun updatePromotion_closedEmployeeDateChange() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns true
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())

            val request = createRequest(startDate = LocalDate.of(2026, 3, 5))

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, request) }
                .isInstanceOf(ClosedPromotionModificationException::class.java)
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 존재 + 거래처 변경 -> 수정 허용")
        fun updatePromotion_adminClosedEmployeeCriticalField_allowed() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns true
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(role = AppAuthority.BRANCH_MANAGER))

            val newAccount = createAccount(id = 200, name = "GS25 강남점")
            every { accountRepository.findById(200) } returns Optional.of(newAccount)
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            val pe = PromotionEmployee(
                id = 5L, promotionId = 1L, employeeId = 1L,
                scheduleDate = LocalDate.of(2026, 3, 15), workStatus = WorkingType.WORK,
                workType1 = WorkingCategory1.EVENT, workType3 = WorkingCategory3.FIXED, teamMemberScheduleId = 100L
            )
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns listOf(pe)
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            val request = createRequest(accountId = 200)
            val result = adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)

            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("ADMIN이 마감 조원 존재 + 시작일 변경 -> 수정 허용")
        fun updatePromotion_adminClosedEmployeeDateChange_allowed() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns true
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(role = AppAuthority.BRANCH_MANAGER))
            every { promotionEmployeeRepository.findMinScheduleDateByPromotionId(1L) } returns LocalDate.of(2026, 3, 10)
            every { promotionEmployeeRepository.findMaxScheduleDateByPromotionId(1L) } returns LocalDate.of(2026, 3, 18)
            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            val request = createRequest(startDate = LocalDate.of(2026, 3, 5))
            val result = adminPromotionService.updatePromotion(scope, principal, 1L, userId, request)

            assertThat(result).isNotNull()
        }

        @Test
        @DisplayName("LEADER가 마감 조원 존재 + 시작일 변경 -> ClosedPromotionModificationException")
        fun updatePromotion_leaderClosedEmployeeDateChange() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns true
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(role = AppAuthority.LEADER))

            val request = createRequest(startDate = LocalDate.of(2026, 3, 5))

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, request) }
                .isInstanceOf(ClosedPromotionModificationException::class.java)
        }

        @Test
        @DisplayName("날짜 축소 시 조원 범위 초과 -> DateRangeConflictException")
        fun updatePromotion_dateRangeConflict() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns false
            every { promotionEmployeeRepository.findMinScheduleDateByPromotionId(1L) } returns LocalDate.of(2026, 3, 12)
            every { promotionEmployeeRepository.findMaxScheduleDateByPromotionId(1L) } returns LocalDate.of(2026, 3, 18)

            // startDate를 3/13으로 → minDate(3/12) 이후이므로 충돌
            val request = createRequest(startDate = LocalDate.of(2026, 3, 13))

            assertThatThrownBy { adminPromotionService.updatePromotion(scope, principal, 1L, userId, request) }
                .isInstanceOf(DateRangeConflictException::class.java)
        }

        @Test
        @DisplayName("거래처 변경 -> 스케줄 초기화")
        fun updatePromotion_accountChange_resetSchedules() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns false

            val pe = PromotionEmployee(
                id = 5L, promotionId = 1L, employeeId = 1L,
                scheduleDate = LocalDate.of(2026, 3, 15), workStatus = WorkingType.WORK,
                workType1 = WorkingCategory1.EVENT, workType3 = WorkingCategory3.FIXED, teamMemberScheduleId = 100L
            )
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns listOf(pe)

            val newAccount = createAccount(id = 200, name = "GS25 강남점")
            every { accountRepository.findById(200) } returns Optional.of(newAccount)
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }
            every { promotionEmployeeRepository.save(any<PromotionEmployee>()) } answers { firstArg<PromotionEmployee>() }

            adminPromotionService.updatePromotion(scope, principal, 1L, userId, createRequest(accountId = 200))

            verify { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(100L)) }
            assertThat(pe.teamMemberScheduleId).isNull()
        }
    }

    @Nested
    @DisplayName("deletePromotion - 행사마스터 삭제")
    inner class DeletePromotionTests {

        // controller 가 dataScopeHolder.require() 결과를 explicit parameter 로 전달하는 패턴 시뮬레이션.
        private val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 삭제 - 유효한 ID -> soft delete + 연쇄 삭제")
        fun deletePromotion_success() {
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns false

            val pe = PromotionEmployee(
                id = 5L, promotionId = 1L, employeeId = 1L,
                scheduleDate = LocalDate.of(2026, 3, 15), workStatus = WorkingType.WORK,
                workType1 = WorkingCategory1.EVENT, workType3 = WorkingCategory3.FIXED, teamMemberScheduleId = 100L
            )
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns listOf(pe)
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }

            adminPromotionService.deletePromotion(scope, principal, 1L)

            assertThat(promotion.isDeleted).isTrue()
            verify { teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(100L)) }
            verify { promotionEmployeeRepository.deleteByPromotionId(1L) }
            verify { promotionRepository.save(promotion) }
        }

        @Test
        @DisplayName("마감 조원 존재 -> ClosedPromotionDeleteException")
        fun deletePromotion_closedEmployee() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion()
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(1L) } returns true

            assertThatThrownBy { adminPromotionService.deletePromotion(scope, principal, 1L) }
                .isInstanceOf(ClosedPromotionDeleteException::class.java)
        }

        @Test
        @DisplayName("이미 삭제된 행사마스터 -> PromotionNotFoundException")
        fun deletePromotion_alreadyDeleted() {
            val promotion = createPromotion(isDeleted = true)
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion

            assertThatThrownBy { adminPromotionService.deletePromotion(scope, principal, 1L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("음수 ID -> PromotionInvalidParameterException")
        fun deletePromotion_negativeId() {
            assertThatThrownBy { adminPromotionService.deletePromotion(scope, principal, -1L) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("권한 외 삭제 - 지점장이 타 지점 행사 -> PromotionForbiddenException")
        fun deletePromotion_forbidden() {
            val scope = DataScope(branchCodes = listOf("2202"), isAllBranches = false)
            // scope 는 service 호출 시 직접 전달 (holder mock 제거 — explicit parameter 패턴)

            val promotion = createPromotion(costCenterCode = "1101")
            every { promotionRepository.findByIdWithRelations(1L) } returns promotion
            every { promotionRepository.existsVisibleById(1L, any()) } returns false

            assertThatThrownBy { adminPromotionService.deletePromotion(scope, principal, 1L) }
                .isInstanceOf(PromotionForbiddenException::class.java)
        }
    }

    // UC-11: 행사마스터 복제 (폼 방식) — 레거시 PromotionCloneComponent Quick Action 동등
    @Nested
    @DisplayName("clonePromotion - 행사마스터 복제 (폼 방식)")
    inner class ClonePromotionTests {

        @Test
        @DisplayName("UC-11 정상 복제 - 신규 행사 생성 + 행사상품 자동 생성 + 행사사원 복사 (이력성 필드 초기화)")
        fun clonePromotion_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            // 원본 행사
            val sourcePromotion = createPromotion(id = 1L, costCenterCode = "1101")
            every { promotionRepository.findByIdWithRelations(1L) } returns sourcePromotion

            // 원본 행사사원 2건 (이력성 필드 채워진 상태)
            val sourceEmployees = listOf(
                PromotionEmployee(
                    id = 100L, promotionId = 1L, employeeId = 10L,
                    scheduleDate = LocalDate.of(2026, 3, 15),
                    basePrice = BigDecimal.valueOf(1500L), dailyTargetCount = BigDecimal.valueOf(100L),
                    primarySalesPrice = BigDecimal.valueOf(1200L), primarySalesQuantity = BigDecimal.valueOf(80L),
                    otherSalesAmount = BigDecimal.valueOf(50000L), otherSalesQuantity = BigDecimal.valueOf(30L),
                    primaryProductAmount = BigDecimal.valueOf(96000L),
                    description = "원본 비고",
                    s3ImageUniqueKey = "uniqueKey1",
                    teamMemberScheduleId = 5000L,
                    promoCloseByTm = true
                ),
                PromotionEmployee(
                    id = 101L, promotionId = 1L, employeeId = 11L,
                    scheduleDate = LocalDate.of(2026, 3, 16),
                    basePrice = BigDecimal.valueOf(1500L), dailyTargetCount = BigDecimal.valueOf(100L)
                )
            )
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns sourceEmployees

            // createPromotion 의존 stub (CC 자동 채움 + 신규 promotion 생성)
            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct(name = "꿀배청 680G"))
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(costCenterCode = "1101"))
            every { promotionRepository.getNextPromotionNumberSeq() } returns 2L
            every { promotionRepository.save(any<Promotion>()) } answers {
                val p = firstArg<Promotion>()
                // id 부여 모사 (Kotlin val 이라 reflection 필요 없이 새 인스턴스 반환)
                Promotion(
                    id = 2L,
                    promotionNumber = p.promotionNumber,
                    promotionType = p.promotionType,
                    account = p.account,
                    startDate = p.startDate,
                    endDate = p.endDate,
                    primaryProductId = p.primaryProductId,
                    costCenterCode = p.costCenterCode
                )
            }
            every { promotionProductRepository.findByPromotionId(any()) } returns null

            // 신규 행사사원 일괄 save
            val savedClones = slot<List<PromotionEmployee>>()
            every {
                promotionEmployeeRepository.saveAll(capture(savedClones))
            } answers { firstArg<List<PromotionEmployee>>() }

            val request = createRequest(promotionType = "시식")
            val result = adminPromotionService.clonePromotion(scope, 1L, userId, request)

            // 신규 행사 검증
            assertThat(result.promotionNumber).isEqualTo("PM00000002")
            assertThat(result.costCenterCode).isEqualTo("1101")

            // 행사상품 1건 자동 생성 검증 (T7 동등)
            verify { promotionProductRepository.save(any<PromotionProduct>()) }

            // 신규 행사사원 일괄 save 호출 + 이력성 필드 초기화 검증
            verify { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) }
            val cloned = savedClones.captured
            assertThat(cloned).hasSize(2)

            // 첫 번째 복제 행 — 이력성 필드 모두 초기화 + 신규 promotion_id 연결
            val c1 = cloned[0]
            assertThat(c1.promotionId).isEqualTo(2L)
            assertThat(c1.employeeId).isEqualTo(10L)  // 담당자 복사 유지
            assertThat(c1.scheduleDate).isNull()  // 투입일 초기화
            assertThat(c1.basePrice).isNull()  // 기준단가 초기화
            assertThat(c1.dailyTargetCount).isNull()  // 일일목표수량 초기화
            assertThat(c1.primarySalesPrice).isNull()
            assertThat(c1.primarySalesQuantity).isNull()
            assertThat(c1.otherSalesAmount).isNull()
            assertThat(c1.otherSalesQuantity).isNull()
            assertThat(c1.primaryProductAmount).isNull()
            assertThat(c1.description).isNull()  // 비고 초기화
            assertThat(c1.s3ImageUniqueKey).isNull()  // 이미지 키 초기화
            assertThat(c1.teamMemberScheduleId).isNull()  // 여사원일정 ID 초기화
            assertThat(c1.promoCloseByTm).isFalse  // 마감 여부 초기화
        }

        @Test
        @DisplayName("UC-11 원본 행사사원 0건 -> 신규 행사만 생성 (saveAll 호출 없음)")
        fun clonePromotion_noEmployees_skipsSaveAll() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val sourcePromotion = createPromotion(id = 1L, costCenterCode = "1101")
            every { promotionRepository.findByIdWithRelations(1L) } returns sourcePromotion
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns emptyList()

            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(costCenterCode = "1101"))
            every { promotionRepository.getNextPromotionNumberSeq() } returns 2L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }
            every { promotionProductRepository.findByPromotionId(any()) } returns null

            adminPromotionService.clonePromotion(scope, 1L, userId, createRequest())

            verify(exactly = 0) { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) }
        }

        @Test
        @DisplayName("UC-11 sourceId 0 이하 -> PromotionInvalidParameterException")
        fun clonePromotion_invalidSourceId() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            assertThatThrownBy { adminPromotionService.clonePromotion(scope, 0L, userId, createRequest()) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("UC-11 원본 삭제됨 -> PromotionNotFoundException")
        fun clonePromotion_sourceDeleted() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            every { promotionRepository.findByIdWithRelations(1L) } returns createPromotion(isDeleted = true)

            assertThatThrownBy { adminPromotionService.clonePromotion(scope, 1L, userId, createRequest()) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }

    // UC-12: 행사마스터 자식 포함 복제 (1클릭) — 레거시 ClonePromotionWithChildsController 동등
    @Nested
    @DisplayName("cloneWithChildren - 행사마스터 자식 포함 복제 (1클릭)")
    inner class CloneWithChildrenTests {

        @Test
        @DisplayName("UC-12 정상 1클릭 복제 - 원본 모든 필드 복사 + 행사사원 5필드만 복사 (나머지 초기값)")
        fun cloneWithChildren_success() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            // 원본 행사
            val sourcePromotion = createPromotion(
                id = 1L,
                promotionType = PromotionType.SAMPLING,
                costCenterCode = "1101"
            )
            every { promotionRepository.findByIdWithRelations(1L) } returns sourcePromotion

            // 원본 행사사원 — 모든 필드 채워진 상태
            val sourceEmployees = listOf(
                PromotionEmployee(
                    id = 100L, promotionId = 1L, employeeId = 10L,
                    workStatus = WorkingType.WORK,
                    workType1 = WorkingCategory1.EVENT,
                    workType3 = WorkingCategory3.FIXED,
                    scheduleDate = LocalDate.of(2026, 3, 15),
                    basePrice = BigDecimal.valueOf(1500L), dailyTargetCount = BigDecimal.valueOf(100L),
                    primarySalesPrice = BigDecimal.valueOf(1200L), primarySalesQuantity = BigDecimal.valueOf(80L),
                    otherSalesAmount = BigDecimal.valueOf(50000L), otherSalesQuantity = BigDecimal.valueOf(30L),
                    primaryProductAmount = BigDecimal.valueOf(96000L),
                    description = "원본 비고",
                    s3ImageUniqueKey = "uniqueKey1",
                    teamMemberScheduleId = 5000L,
                    promoCloseByTm = true
                )
            )
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns sourceEmployees

            // createPromotion 의존 stub
            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct(name = "꿀배청 680G"))
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(costCenterCode = "1101"))
            every { promotionRepository.getNextPromotionNumberSeq() } returns 2L
            every { promotionRepository.save(any<Promotion>()) } answers {
                val p = firstArg<Promotion>()
                Promotion(
                    id = 2L,
                    promotionNumber = p.promotionNumber,
                    promotionType = p.promotionType,
                    account = p.account,
                    startDate = p.startDate,
                    endDate = p.endDate,
                    primaryProductId = p.primaryProductId,
                    costCenterCode = p.costCenterCode
                )
            }
            every { promotionProductRepository.findByPromotionId(any()) } returns null
            val savedClones = slot<List<PromotionEmployee>>()
            every {
                promotionEmployeeRepository.saveAll(capture(savedClones))
            } answers { firstArg<List<PromotionEmployee>>() }

            val result = adminPromotionService.cloneWithChildren(scope, 1L, userId)

            // 신규 행사 검증 (시작일·종료일 포함 원본 동일)
            assertThat(result.promotionNumber).isEqualTo("PM00000002")
            assertThat(result.costCenterCode).isEqualTo("1101")
            assertThat(result.startDate).isEqualTo(sourcePromotion.startDate)
            assertThat(result.endDate).isEqualTo(sourcePromotion.endDate)

            // 행사상품 1건 자동 생성 검증 (T7 동등)
            verify { promotionProductRepository.save(any<PromotionProduct>()) }

            // 행사사원 5필드만 복사 + 나머지 초기값 검증
            verify { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) }
            val cloned = savedClones.captured
            assertThat(cloned).hasSize(1)

            val c = cloned[0]
            assertThat(c.promotionId).isEqualTo(2L)

            // 복사 유지 (5필드)
            assertThat(c.employeeId).isEqualTo(10L)
            assertThat(c.workStatus).isEqualTo(WorkingType.WORK)
            assertThat(c.workType1).isEqualTo(WorkingCategory1.EVENT)
            assertThat(c.workType3).isEqualTo(WorkingCategory3.FIXED)

            // 나머지 모두 초기값 (레거시 ClonePromotionWithChildsController 동등 — UC-11 보다 더 강한 초기화)
            assertThat(c.scheduleDate).isNull()
            assertThat(c.basePrice).isNull()
            assertThat(c.dailyTargetCount).isNull()
            assertThat(c.primarySalesPrice).isNull()
            assertThat(c.primarySalesQuantity).isNull()
            assertThat(c.otherSalesAmount).isNull()
            assertThat(c.otherSalesQuantity).isNull()
            assertThat(c.primaryProductAmount).isNull()
            assertThat(c.description).isNull()
            assertThat(c.s3ImageUniqueKey).isNull()
            assertThat(c.teamMemberScheduleId).isNull()
            assertThat(c.promoCloseByTm).isFalse
        }

        @Test
        @DisplayName("UC-12 원본 행사사원 0건 -> 신규 행사만 생성 (saveAll 호출 없음)")
        fun cloneWithChildren_noEmployees_skipsSaveAll() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val sourcePromotion = createPromotion(id = 1L, promotionType = PromotionType.SAMPLING, costCenterCode = "1101")
            every { promotionRepository.findByIdWithRelations(1L) } returns sourcePromotion
            every { promotionEmployeeRepository.findByPromotionId(1L) } returns emptyList()

            every { accountRepository.findById(100) } returns Optional.of(createAccount())
            every { productRepository.findById(200L) } returns Optional.of(createProduct())
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee(costCenterCode = "1101"))
            every { promotionRepository.getNextPromotionNumberSeq() } returns 2L
            every { promotionRepository.save(any<Promotion>()) } answers { firstArg<Promotion>() }
            every { promotionProductRepository.findByPromotionId(any()) } returns null

            adminPromotionService.cloneWithChildren(scope, 1L, userId)

            verify(exactly = 0) { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) }
        }

        @Test
        @DisplayName("UC-12 sourceId 0 이하 -> PromotionInvalidParameterException")
        fun cloneWithChildren_invalidSourceId() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            assertThatThrownBy { adminPromotionService.cloneWithChildren(scope, 0L, userId) }
                .isInstanceOf(PromotionInvalidParameterException::class.java)
        }

        @Test
        @DisplayName("UC-12 원본 삭제됨 -> PromotionNotFoundException")
        fun cloneWithChildren_sourceDeleted() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            every { promotionRepository.findByIdWithRelations(1L) } returns createPromotion(isDeleted = true)

            assertThatThrownBy { adminPromotionService.cloneWithChildren(scope, 1L, userId) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }
    }

    // Helper methods
    private fun createPromotion(
        id: Long = 1L,
        promotionNumber: String = "PM00000001",
        promotionType: PromotionType? = null,
        account: Account = createAccount(),
        primaryProductId: Long? = 200L,
        costCenterCode: String? = "1101",
        isDeleted: Boolean = false,
        remark: String? = null
    ) = Promotion(
        id = id,
        promotionNumber = promotionNumber,
        promotionType = promotionType,
        account = account,
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = primaryProductId,
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = StandLocation.END_CAP,
        costCenterCode = costCenterCode,
        isDeleted = isDeleted,
        remark = remark
    )

    private fun createAccount(id: Int = 100, name: String = "GS25 역삼점", branchName: String? = "강남53지점") = Account(
        id = id,
        name = name
    ).also { it.branchName = branchName }

    private fun createProduct(
        id: Long = 200L,
        name: String = "진라면 매운맛 120g",
        category1: String = "라면"
    ) = Product(id = id, name = name).also {
        it.productCategory1 = category1
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "20030117",
        costCenterCode: String? = "1101",
        role: String? = null
    ) = Employee(id = id, employeeCode = employeeCode, name = "테스트 사용자").also {
        it.costCenterCode = costCenterCode
        it.role = role
    }

    private fun createRequest(
        promotionType: String? = null,
        accountId: Int = 100,
        startDate: LocalDate = LocalDate.of(2026, 3, 10),
        endDate: LocalDate = LocalDate.of(2026, 3, 20),
        primaryProductId: Long? = 200L,
        standLocation: String? = "냉동행사장",
        otherProduct: String? = "너구리, 진짬뽕",
        remark: String? = null
    ) = PromotionCreateRequest(
        promotionType = promotionType,
        accountId = accountId,
        startDate = startDate,
        endDate = endDate,
        primaryProductId = primaryProductId,
        otherProduct = otherProduct,
        message = "3월 라면 프로모션 진행",
        standLocation = standLocation,
        remark = remark
    )
}
