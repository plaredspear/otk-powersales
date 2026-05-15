package com.otoki.powersales.promotion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.promotion.dto.request.PromotionCreateRequest
import com.otoki.powersales.promotion.dto.response.*
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.promotion.enums.ProductTemperatureType
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.enums.PromotionType
import com.otoki.powersales.promotion.enums.StandLocation
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
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
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val employeeRepository: EmployeeRepository,
    private val dataScopeHolder: DataScopeHolder,
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

    fun getPromotions(
        keyword: String?,
        promotionType: String?,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int
    ): PromotionListResponse {
        val scope = dataScopeHolder.require()

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

    fun getPromotion(id: Long): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(promotion)

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
                remark = request.remark
            )
        )

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account.name,
            primaryProductName = product.name
        )
    }

    @Transactional
    fun updatePromotion(id: Long, userId: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(promotion)
        validateDateRange(request.startDate, request.endDate)
        val promotionType = parsePromotionType(request.promotionType)
        validateStandLocation(request.standLocation)
        validateOtherProduct(request.otherProduct)

        // 1-2-C: 마감 보호 — 거래처/날짜 변경 차단 (ADMIN 예외)
        val criticalFieldChanged = promotion.account.id != request.accountId ||
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
        if (promotion.account.id != request.accountId) {
            resetSchedulesForPromotion(id)
        }

        val account = accountRepository.findById(request.accountId)
            .orElseThrow { AccountNotFoundException() }

        val product = productRepository.findById(request.primaryProductId!!)
            .orElseThrow { ProductNotFoundException() }

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
            remark = request.remark
        )

        promotionRepository.save(promotion)

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account?.name,
            primaryProductName = product.name
        )
    }

    @Transactional
    fun deletePromotion(id: Long) {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(promotion)

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

    private fun validateDataScope(promotion: Promotion) {
        val scope = dataScopeHolder.require()
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
