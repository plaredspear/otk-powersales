package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.EffectiveBranchResult
import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.admin.dto.response.PromotionDetailResponse
import com.otoki.internal.admin.dto.response.PromotionListItem
import com.otoki.internal.admin.dto.response.PromotionListResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionProduct
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionProductRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.promotion.repository.PromotionTypeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.ProductRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.repository.ScheduleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionService(
    private val promotionRepository: PromotionRepository,
    private val promotionProductRepository: PromotionProductRepository,
    private val promotionTypeRepository: PromotionTypeRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val dataScopeHolder: DataScopeHolder,
    private val scheduleRepository: ScheduleRepository
) {

    fun getPromotions(
        keyword: String?,
        promotionTypeId: Long?,
        category: String?,
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
            promotionTypeId = promotionTypeId,
            category = category,
            startDate = startDate,
            endDate = endDate,
            branchCodes = effectiveBranchCodes,
            pageable = pageable
        )

        val accountIds = promotionPage.content.map { it.accountId }.distinct()
        val accountMap = if (accountIds.isNotEmpty()) {
            accountRepository.findByIdIn(accountIds).associateBy { it.id }
        } else emptyMap()

        val typeIds = promotionPage.content.mapNotNull { it.promotionTypeId }.distinct()
        val typeMap = if (typeIds.isNotEmpty()) {
            promotionTypeRepository.findAllById(typeIds).associateBy { it.id }
        } else emptyMap()

        return PromotionListResponse(
            content = promotionPage.content.map { promotion ->
                val account = accountMap[promotion.accountId]
                val typeName = promotion.promotionTypeId?.let { typeMap[it]?.name }
                PromotionListItem.from(
                    promotion = promotion,
                    accountName = account?.name,
                    promotionTypeName = typeName
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

        val account = accountRepository.findById(promotion.accountId).orElse(null)
        val product = promotion.primaryProductId?.let { productRepository.findById(it).orElse(null) }
        val typeName = promotion.promotionTypeId?.let {
            promotionTypeRepository.findById(it).orElse(null)?.name
        }

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account?.name,
            primaryProductName = product?.name,
            promotionTypeName = typeName
        )
    }

    @Transactional
    fun createPromotion(userId: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        validateDateRange(request.startDate, request.endDate)
        validatePromotionType(request.promotionTypeId)

        val account = accountRepository.findById(request.accountId)
            .orElseThrow { AccountNotFoundException() }

        val product = request.primaryProductId?.let {
            productRepository.findById(it).orElseThrow { ProductNotFoundException() }
        }

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }

        val costCenterCode = user.costCenterCode
            ?: throw CostCenterNotFoundException()

        val seq = promotionRepository.getNextPromotionNumberSeq()
        val promotionNumber = "PM" + String.format("%08d", seq)

        val typeName = request.promotionTypeId?.let {
            promotionTypeRepository.findById(it).orElse(null)?.name
        }

        val promotion = promotionRepository.save(
            Promotion(
                promotionNumber = promotionNumber,
                promotionName = request.promotionName,
                promotionTypeId = request.promotionTypeId,
                accountId = request.accountId,
                startDate = request.startDate,
                endDate = request.endDate,
                primaryProductId = request.primaryProductId,
                otherProduct = request.otherProduct,
                message = request.message,
                standLocation = request.standLocation,
                targetAmount = 0,
                actualAmount = 0,
                costCenterCode = costCenterCode,
                category = request.category,
                productType = request.productType,
                branchName = request.branchName,
                professionalTeam = request.professionalTeam,
                externalId = request.externalId,
                remark = request.remark
            )
        )

        if (request.primaryProductId != null) {
            promotionProductRepository.save(
                PromotionProduct(
                    promotionId = promotion.id,
                    productId = request.primaryProductId
                )
            )
        }

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account.name,
            primaryProductName = product?.name,
            promotionTypeName = typeName
        )
    }

    @Transactional
    fun updatePromotion(id: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(promotion)
        validateDateRange(request.startDate, request.endDate)
        validatePromotionType(request.promotionTypeId)

        // 1-2-C: 마감 보호 — 거래처/날짜 변경 차단
        val criticalFieldChanged = promotion.accountId != request.accountId ||
            promotion.startDate != request.startDate ||
            promotion.endDate != request.endDate
        if (criticalFieldChanged) {
            if (promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(id)) {
                throw ClosedPromotionModificationException()
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
        if (promotion.accountId != request.accountId) {
            resetSchedulesForPromotion(id)
        }

        accountRepository.findById(request.accountId)
            .orElseThrow { AccountNotFoundException() }

        val product = request.primaryProductId?.let {
            productRepository.findById(it).orElseThrow { ProductNotFoundException() }
        }

        val oldPrimaryProductId = promotion.primaryProductId

        promotion.update(
            promotionName = request.promotionName,
            promotionTypeId = request.promotionTypeId,
            accountId = request.accountId,
            startDate = request.startDate,
            endDate = request.endDate,
            primaryProductId = request.primaryProductId,
            otherProduct = request.otherProduct,
            message = request.message,
            standLocation = request.standLocation,
            category = request.category,
            productType = request.productType,
            branchName = request.branchName,
            professionalTeam = request.professionalTeam,
            externalId = request.externalId,
            remark = request.remark
        )

        promotionRepository.save(promotion)

        // 대표상품 변경 처리
        if (oldPrimaryProductId != request.primaryProductId) {
            val existingPP = promotionProductRepository.findByPromotionIdAndIsMainProduct(promotion.id, true)

            when {
                request.primaryProductId != null && existingPP != null -> {
                    existingPP.productId = request.primaryProductId
                    promotionProductRepository.save(existingPP)
                }
                request.primaryProductId != null && existingPP == null -> {
                    promotionProductRepository.save(
                        PromotionProduct(
                            promotionId = promotion.id,
                            productId = request.primaryProductId
                        )
                    )
                }
                request.primaryProductId == null && existingPP != null -> {
                    promotionProductRepository.delete(existingPP)
                }
            }
        }

        val account = accountRepository.findById(promotion.accountId).orElse(null)
        val typeName = promotion.promotionTypeId?.let {
            promotionTypeRepository.findById(it).orElse(null)?.name
        }

        return PromotionDetailResponse.from(
            promotion = promotion,
            accountName = account?.name,
            primaryProductName = product?.name,
            promotionTypeName = typeName
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
        val scheduleIds = employees.mapNotNull { it.scheduleId }
        if (scheduleIds.isNotEmpty()) {
            scheduleRepository.deleteAllByIdIn(scheduleIds)
        }
        promotionEmployeeRepository.deleteByPromotionId(id)

        promotion.softDelete()
        promotionRepository.save(promotion)
    }

    private fun resetSchedulesForPromotion(promotionId: Long) {
        val employees = promotionEmployeeRepository.findByPromotionId(promotionId)
        val scheduleIds = employees.mapNotNull { it.scheduleId }
        if (scheduleIds.isNotEmpty()) {
            scheduleRepository.deleteAllByIdIn(scheduleIds)
        }
        employees.forEach { pe ->
            if (pe.scheduleId != null) {
                pe.scheduleId = null
                promotionEmployeeRepository.save(pe)
            }
        }
    }

    private fun validatePromotionType(promotionTypeId: Long?) {
        if (promotionTypeId != null) {
            val type = promotionTypeRepository.findById(promotionTypeId).orElse(null)
            if (type == null || !type.isActive) {
                throw InvalidPromotionTypeException()
            }
        }
    }

    private fun findActivePromotion(id: Long): Promotion {
        val promotion = promotionRepository.findById(id)
            .orElseThrow { PromotionNotFoundException() }
        if (promotion.isDeleted) throw PromotionNotFoundException()
        return promotion
    }

    private fun validateDataScope(promotion: Promotion) {
        val scope = dataScopeHolder.require()
        if (!scope.validateAccess(promotion.costCenterCode)) {
            throw PromotionForbiddenException()
        }
    }

    private fun validateDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate) {
        if (endDate.isBefore(startDate)) throw InvalidDateRangeException()
    }

    private fun emptyResponse(page: Int, size: Int) = PromotionListResponse(
        content = emptyList(),
        page = page,
        size = size,
        totalElements = 0,
        totalPages = 0
    )
}
