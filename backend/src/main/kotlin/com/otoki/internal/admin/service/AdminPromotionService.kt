package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.admin.dto.response.PromotionDetailResponse
import com.otoki.internal.admin.dto.response.PromotionListItem
import com.otoki.internal.admin.dto.response.PromotionListResponse
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionProduct
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionProductRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.promotion.repository.PromotionTypeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.ProductRepository
import com.otoki.internal.sap.repository.UserRepository
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
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val adminDataScopeService: AdminDataScopeService
) {

    fun getPromotions(
        userId: Long,
        keyword: String?,
        promotionTypeId: Long?,
        category: String?,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int
    ): PromotionListResponse {
        val scope = adminDataScopeService.resolve(userId)

        val effectiveBranchCodes: List<String>? = when {
            scope.isAllBranches -> null
            else -> scope.branchCodes.ifEmpty { return emptyResponse(page, size) }
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

    fun getPromotion(userId: Long, id: Long): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(userId, promotion)

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
                targetAmount = request.targetAmount,
                costCenterCode = costCenterCode,
                category = request.category,
                productType = request.productType,
                branchName = request.branchName,
                professionalTeam = request.professionalTeam,
                externalId = request.externalId
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
    fun updatePromotion(userId: Long, id: Long, request: PromotionCreateRequest): PromotionDetailResponse {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(userId, promotion)
        validateDateRange(request.startDate, request.endDate)
        validatePromotionType(request.promotionTypeId)

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
            targetAmount = request.targetAmount,
            category = request.category,
            productType = request.productType,
            branchName = request.branchName,
            professionalTeam = request.professionalTeam,
            externalId = request.externalId
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
    fun deletePromotion(userId: Long, id: Long) {
        if (id <= 0) throw PromotionInvalidParameterException()

        val promotion = findActivePromotion(id)
        validateDataScope(userId, promotion)

        promotion.softDelete()
        promotionRepository.save(promotion)
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

    private fun validateDataScope(userId: Long, promotion: Promotion) {
        val scope = adminDataScopeService.resolve(userId)
        if (!scope.isAllBranches) {
            if (promotion.costCenterCode == null || promotion.costCenterCode !in scope.branchCodes) {
                throw PromotionForbiddenException()
            }
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
