package com.otoki.powersales.promotion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.promotion.dto.request.PromotionCreateRequest
import com.otoki.powersales.promotion.dto.response.*
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.promotion.enums.ProductTemperatureType
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.entity.PromotionProduct
import com.otoki.powersales.promotion.enums.PromotionType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionProductRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionService(
    private val promotionRepository: PromotionRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionProductRepository: PromotionProductRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) {

    fun getPromotionFormMeta(): PromotionFormMetaResponse {
        val promotionTypes = PromotionType.entries
            .sortedBy { it.displayOrder }
            .map { PromotionTypeOption(value = it.name, name = it.displayName) }

        val standLocations = StandLocation.entries
            .sortedBy { it.displayOrder }
            .map { StandLocationOption(value = it.name, name = it.displayName) }

        return PromotionFormMetaResponse(
            promotionTypes = promotionTypes,
            standLocations = standLocations
        )
    }

    /**
     * @param scope 호출자(controller) 에서 산출/주입한 현재 사용자의 DataScope.
     *              service 가 holder/ambient context 에 의존하지 않도록 explicit parameter 로 받는다.
     */
    fun getPromotions(
        scope: DataScope,
        keyword: String?,
        promotionType: String?,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int
    ): PromotionListResponse {
        val effectiveBranchCodes: List<String>? = when (val result = scope.effectiveBranchCodes(null)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyResponse(page, size)
        }

        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val promotionPage = promotionRepository.searchForAdmin(
            keyword = keyword,
            promotionType = PromotionType.fromDisplayNameOrNull(promotionType),
            startDate = startDate,
            endDate = endDate,
            branchCodes = effectiveBranchCodes,
            pageable = pageable
        )

        return PromotionListResponse(
            content = promotionPage.content.map { promotion ->
                PromotionListItem.from(
                    promotion = promotion,
                    accountName = promotion.account?.name
                )
            },
            page = page,
            size = size,
            totalElements = promotionPage.totalElements,
            totalPages = promotionPage.totalPages
        )
    }

    fun getPromotion(scope: DataScope, id: Long): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(scope, promotion)

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = promotion.account?.name,
            primaryProductName = promotion.primaryProduct?.name
        )
    }

    @Transactional
    fun createPromotion(userId: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        validateDateRange(request.startDate, request.endDate)
        val promotionType = parsePromotionType(request.promotionType)
        validateStandLocation(request.standLocation)
        validateOtherProduct(request.otherProduct)

        val account = accountRepository.findById(request.accountId)
            .orElseThrow { AccountNotFoundException() }

        val product = productRepository.findById(request.primaryProductId!!)
            .orElseThrow { ProductNotFoundException() }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }

        val costCenterCode = employee.costCenterCode
            ?: throw CostCenterNotFoundException()

        val seq = promotionRepository.getNextPromotionNumberSeq()
        val promotionNumber = "PM" + String.format("%08d", seq)

        val promotion = promotionRepository.save(
            Promotion(
                promotionNumber = promotionNumber,
                promotionType = promotionType,
                account = account,
                startDate = request.startDate,
                endDate = request.endDate,
                primaryProductId = request.primaryProductId,
                otherProduct = request.otherProduct,
                message = request.message,
                standLocation = StandLocation.fromDisplayNameOrNull(request.standLocation),
                costCenterCode = costCenterCode,
                productType = ProductTemperatureType.fromDisplayNameOrNull(request.productType),
                category1 = request.category1,
                remark = request.remark
            )
        )

        // 레거시 PromotionTriggerHandler.insertPromotionProduct 동등 (afterInsert)
        // 행사마스터 1건당 대표품목을 가리키는 행사상품 (DKRetail__PromotionProduct__c) 1건 자동 생성
        upsertPromotionProduct(promotion.id, request.primaryProductId)

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account.name,
            primaryProductName = product.name
        )
    }

    @Transactional
    fun updatePromotion(scope: DataScope, id: Long, userId: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(scope, promotion)
        validateDateRange(request.startDate, request.endDate)
        val promotionType = parsePromotionType(request.promotionType)
        validateStandLocation(request.standLocation)
        validateOtherProduct(request.otherProduct)

        // 1-2-C: 마감 보호 — 거래처/날짜 변경 차단 (ADMIN 예외)
        val criticalFieldChanged = promotion.account!!.id != request.accountId ||
            promotion.startDate != request.startDate ||
            promotion.endDate != request.endDate
        if (criticalFieldChanged) {
            if (promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(id)) {
                val employee = employeeRepository.findById(userId)
                    .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
                if (employee.role != UserRole.BRANCH_MANAGER) {
                    throw ClosedPromotionModificationException()
                }
            }
        }

        // 2: 날짜 축소 보호
        if (promotion.startDate != request.startDate || promotion.endDate != request.endDate) {
            val minDate = promotionEmployeeRepository.findMinScheduleDateByPromotionId(id)
            val maxDate = promotionEmployeeRepository.findMaxScheduleDateByPromotionId(id)
            if (minDate != null && maxDate != null) {
                if (request.startDate.isAfter(minDate) || request.endDate.isBefore(maxDate)) {
                    throw DateRangeConflictException(minDate.toString(), maxDate.toString())
                }
            }
        }

        // 1-3: 거래처 변경 시 스케줄 초기화
        if (promotion.account!!.id != request.accountId) {
            resetSchedulesForPromotion(id)
        }

        val account = accountRepository.findById(request.accountId)
            .orElseThrow { AccountNotFoundException() }

        val product = productRepository.findById(request.primaryProductId!!)
            .orElseThrow { ProductNotFoundException() }

        // 레거시 PromotionTriggerHandler.changePosProduct 동등 (afterUpdate)
        // 대표품목 변경 시 행사상품 (DKRetail__PromotionProduct__c) 자동 upsert
        val primaryProductChanged = promotion.primaryProductId != request.primaryProductId

        promotion.update(
            promotionType = promotionType,
            account = account,
            startDate = request.startDate,
            endDate = request.endDate,
            primaryProductId = request.primaryProductId,
            otherProduct = request.otherProduct,
            message = request.message,
            standLocation = StandLocation.fromDisplayNameOrNull(request.standLocation),
            productType = ProductTemperatureType.fromDisplayNameOrNull(request.productType),
            category1 = request.category1,
            remark = request.remark
        )

        promotionRepository.save(promotion)

        if (primaryProductChanged) {
            upsertPromotionProduct(promotion.id, request.primaryProductId)
        }

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account?.name,
            primaryProductName = product.name
        )
    }

    @Transactional
    fun deletePromotion(scope: DataScope, id: Long) {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(scope, promotion)

        // 1-2-D: 마감 보호 — 삭제 차단
        if (promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(id)) {
            throw ClosedPromotionDeleteException()
        }

        // 1-4-A: 연쇄 삭제 — 스케줄 + 조원
        val employees = promotionEmployeeRepository.findByPromotionId(id)
        val scheduleIds = employees.mapNotNull { it.teamMemberScheduleId }
        if (scheduleIds.isNotEmpty()) {
            teamMemberScheduleRepository.deleteAllByIdIn(scheduleIds)
        }
        promotionEmployeeRepository.deleteByPromotionId(id)

        promotion.softDelete()
        promotionRepository.save(promotion)
    }

    /**
     * UC-11: 행사마스터 복제 — 폼 방식.
     * 레거시 PromotionCloneComponentController.createRecord 동등.
     *
     * 사용자가 폼에서 수정한 신규 행사 정보로 신규 행사마스터 생성 (CC 자동 채움 + 기타제품 검증 + 행사상품 자동 생성).
     * 원본 행사사원 모두 복제하되 이력성 필드 (실적·일정·마감·이미지·비고·기준단가·목표수량) 11개 초기화.
     */
    @Transactional
    fun clonePromotion(scope: DataScope, sourceId: Long, userId: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        if (sourceId <= 0) throw PromotionInvalidParameterException()

        // 원본 조회 + 권한 검증
        val source = findActivePromotion(sourceId)
        validateDataScope(scope, source)

        // 신규 행사 생성 — createPromotion 동일 흐름 (CC 자동 채움, 기타제품 검증, 행사상품 자동 생성 등 모두 동일 적용)
        val created = createPromotion(userId, request)

        // 원본 행사사원 복제 (이력성 필드 초기화)
        val sourceEmployees = promotionEmployeeRepository.findByPromotionId(sourceId)
        if (sourceEmployees.isNotEmpty()) {
            val cloned = sourceEmployees.map { src ->
                PromotionEmployee(
                    promotionId = created.id,
                    employeeId = src.employeeId,
                    workStatus = src.workStatus,
                    workType1 = src.workType1,
                    workType3 = src.workType3,
                    dkWorkType2 = src.dkWorkType2,
                    // 이력성 필드 초기화 (레거시 PromotionCloneComponentController 동등)
                    scheduleDate = null,
                    basePrice = null,
                    dailyTargetCount = null,
                    targetAmount = null,
                    actualAmount = null,
                    primaryProductAmount = null,
                    primarySalesQuantity = null,
                    primarySalesPrice = null,
                    otherSalesAmount = null,
                    otherSalesQuantity = null,
                    s3ImageUniqueKey = null,
                    description = null,
                    teamMemberScheduleId = null,
                    promoCloseByTm = false
                )
            }
            promotionEmployeeRepository.saveAll(cloned)
        }

        return created
    }

    /**
     * UC-12: 행사마스터 자식 포함 복제 — 1클릭 방식.
     * 레거시 ClonePromotionWithChildsController.doClone 동등.
     *
     * 원본 행사마스터의 모든 필드 + startDate/endDate (옵션 A — 원본 그대로) 복사하여 신규 행사 생성.
     * createPromotion 위임으로 T1 (CC 자동) + T2 (홑따옴표 검증) + T7 (행사상품 자동 생성) 일관 적용.
     * 원본 행사사원은 5필드만 (employeeId, workStatus, workType1/2/3) 복사, 나머지는 모두 초기값.
     */
    @Transactional
    fun cloneWithChildren(scope: DataScope, sourceId: Long, userId: Long): PromotionDetailResponse {
        if (sourceId <= 0) throw PromotionInvalidParameterException()

        // 원본 조회 + 권한 검증
        val source = findActivePromotion(sourceId)
        validateDataScope(scope, source)

        // 원본 필드 → PromotionCreateRequest 변환 (사용자 입력 없는 1클릭)
        // 각 enum 은 fromDisplayName 으로 역변환되므로 displayName 으로 직렬화
        val clonedRequest = PromotionCreateRequest(
            promotionType = source.promotionType?.displayName,
            accountId = source.account!!.id,
            startDate = source.startDate,
            endDate = source.endDate,
            primaryProductId = source.primaryProductId,
            otherProduct = source.otherProduct,
            message = source.message,
            standLocation = source.standLocation?.displayName,
            productType = source.productType?.displayName,
            category1 = source.category1,
            remark = source.remark
        )

        // 신규 행사 생성 (CC 자동 채움 + 검증 + 행사상품 자동 생성 모두 적용)
        val created = createPromotion(userId, clonedRequest)

        // 원본 행사사원 5필드만 복사 (레거시 ClonePromotionWithChildsController 동등)
        val sourceEmployees = promotionEmployeeRepository.findByPromotionId(sourceId)
        if (sourceEmployees.isNotEmpty()) {
            val cloned = sourceEmployees.map { src ->
                PromotionEmployee(
                    promotionId = created.id,
                    employeeId = src.employeeId,
                    workStatus = src.workStatus,
                    workType1 = src.workType1,
                    workType3 = src.workType3,
                    dkWorkType2 = src.dkWorkType2
                    // 나머지 모두 초기값 (scheduleDate / basePrice / dailyTargetCount /
                    // primarySalesPrice / primarySalesQuantity / otherSalesAmount / otherSalesQuantity /
                    // primaryProductAmount / description / s3ImageUniqueKey / teamMemberScheduleId / promoCloseByTm)
                )
            }
            promotionEmployeeRepository.saveAll(cloned)
        }

        return created
    }

    /**
     * 행사상품 (DKRetail__PromotionProduct__c) 자동 upsert — 행사 1건당 1건만 유지.
     * 레거시 PromotionTriggerHandler.insertPromotionProduct (afterInsert) + changePosProduct (afterUpdate) 동등.
     */
    private fun upsertPromotionProduct(promotionId: Long, productId: Long?) {
        val existing = promotionProductRepository.findByPromotionId(promotionId)
        if (existing != null) {
            existing.updateProduct(productId)
            promotionProductRepository.save(existing)
        } else {
            promotionProductRepository.save(
                PromotionProduct(
                    promotionId = promotionId,
                    productId = productId
                )
            )
        }
    }

    private fun resetSchedulesForPromotion(promotionId: Long) {
        val employees = promotionEmployeeRepository.findByPromotionId(promotionId)
        val scheduleIds = employees.mapNotNull { it.teamMemberScheduleId }
        if (scheduleIds.isNotEmpty()) {
            teamMemberScheduleRepository.deleteAllByIdIn(scheduleIds)
        }
        employees.forEach { pe ->
            if (pe.teamMemberScheduleId != null) {
                pe.teamMemberScheduleId = null
                promotionEmployeeRepository.save(pe)
            }
        }
    }

    private fun parsePromotionType(value: String?): PromotionType? {
        if (value.isNullOrBlank()) return null
        return PromotionType.fromDisplayNameOrNull(value)
            ?: throw PromotionInvalidParameterException()
    }

    private fun findActivePromotion(id: Long): Promotion {
        val promotion = promotionRepository.findByIdWithRelations(id)
            ?: throw PromotionNotFoundException()
        if (promotion.isDeleted) throw PromotionNotFoundException()
        return promotion
    }

    private fun validateDataScope(scope: DataScope, promotion: Promotion) {
        if (!scope.validateAccess(promotion.costCenterCode)) {
            throw PromotionForbiddenException()
        }
    }

    private fun validateStandLocation(standLocation: String?) {
        if (standLocation != null && StandLocation.fromDisplayName(standLocation) == null) {
            throw InvalidStandLocationException()
        }
    }

    private fun validateDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate) {
        if (endDate.isBefore(startDate)) throw InvalidDateRangeException()
    }

    private fun validateOtherProduct(otherProduct: String?) {
        if (otherProduct != null && otherProduct.contains("'")) {
            throw InvalidOtherProductException()
        }
    }

    private fun emptyResponse(page: Int, size: Int) = PromotionListResponse(
        content = emptyList(),
        page = page,
        size = size,
        totalElements = 0,
        totalPages = 0
    )
}
