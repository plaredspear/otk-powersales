package com.otoki.internal.promotion.service

import com.otoki.internal.promotion.dto.response.*
import com.otoki.internal.promotion.exception.PromotionForbiddenException
import com.otoki.internal.promotion.exception.PromotionInvalidParameterException
import com.otoki.internal.promotion.exception.PromotionNotFoundException
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.promotion.repository.PromotionTypeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.ProductRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
@Transactional(readOnly = true)
class MobilePromotionService(
    private val promotionRepository: PromotionRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionTypeRepository: PromotionTypeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val employeeRepository: EmployeeRepository
) {

    fun getPromotions(
        userId: Long,
        startDate: String?,
        endDate: String?,
        keyword: String?,
        page: Int,
        size: Int
    ): MobilePromotionListResponse {
        validateDateFormat(startDate)
        validateDateFormat(endDate)
        if (!keyword.isNullOrBlank() && keyword.length > 100) {
            throw PromotionInvalidParameterException()
        }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }

        val isWoman = employee.appAuthority == "여사원"

        val pageable = PageRequest.of(page, size)
        val promotionPage = promotionRepository.searchForMobile(
            employeeId = employee.id,
            costCenterCode = employee.costCenterCode,
            isWoman = isWoman,
            keyword = keyword,
            startDate = startDate,
            endDate = endDate,
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

        return MobilePromotionListResponse(
            content = promotionPage.content.map { promotion ->
                val accountName = accountMap[promotion.accountId]?.name
                val typeName = promotion.promotionTypeId?.let { typeMap[it]?.name }
                val myScheduleDate = if (isWoman) {
                    promotionEmployeeRepository.findMinScheduleDateByPromotionIdAndEmployeeId(
                        promotion.id, employee.id
                    )
                } else null

                MobilePromotionListItem.from(
                    promotion = promotion,
                    accountName = accountName,
                    promotionTypeName = typeName,
                    myScheduleDate = myScheduleDate
                )
            },
            page = page,
            size = size,
            totalElements = promotionPage.totalElements,
            totalPages = promotionPage.totalPages
        )
    }

    fun getPromotion(userId: Long, promotionId: Long): MobilePromotionDetailResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }

        val promotion = promotionRepository.findById(promotionId)
            .filter { !it.isDeleted }
            .orElseThrow { PromotionNotFoundException() }

        val isWoman = employee.appAuthority == "여사원"

        // 권한 검증
        if (isWoman) {
            if (!promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(promotionId, employee.id)) {
                throw PromotionForbiddenException()
            }
        } else {
            if (employee.costCenterCode != promotion.costCenterCode) {
                throw PromotionForbiddenException()
            }
        }

        val account = accountRepository.findById(promotion.accountId).orElse(null)
        val product = promotion.primaryProductId?.let { productRepository.findById(it).orElse(null) }
        val typeName = promotion.promotionTypeId?.let {
            promotionTypeRepository.findById(it).orElse(null)?.name
        }

        // 조원 목록 + 사원명 매핑 (fetchJoin으로 N+1 방지)
        val employees = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId)

        val employeeItems = employees.map { emp ->
            MobilePromotionEmployeeItem.from(emp, emp.employee?.name)
        }

        return MobilePromotionDetailResponse.from(
            promotion = promotion,
            accountName = account?.name,
            primaryProductName = product?.name,
            promotionTypeName = typeName,
            employees = employeeItems
        )
    }

    private fun validateDateFormat(dateStr: String?) {
        if (dateStr.isNullOrBlank()) return
        try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            throw PromotionInvalidParameterException()
        }
    }
}
