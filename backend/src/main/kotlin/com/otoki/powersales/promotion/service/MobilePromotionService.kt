package com.otoki.powersales.promotion.service

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.promotion.dto.response.*
import com.otoki.powersales.promotion.exception.PromotionForbiddenException
import com.otoki.powersales.promotion.exception.PromotionInvalidParameterException
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
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
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val employeeRepository: EmployeeRepository
) {

    fun getPromotions(
        userId: Long,
        startDate: String?,
        endDate: String?,
        keyword: String?,
        accountId: Long?,
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

        val isWoman = employee.role == AppAuthority.WOMAN

        val pageable = PageRequest.of(page, size)
        val promotionPage = promotionRepository.searchForMobile(
            employeeId = employee.id,
            costCenterCode = employee.costCenterCode,
            isWoman = isWoman,
            keyword = keyword,
            startDate = startDate,
            endDate = endDate,
            accountId = accountId,
            pageable = pageable
        )

        val accountIds = promotionPage.content.map { it.account!!.id }.distinct()
        val accountMap = if (accountIds.isNotEmpty()) {
            accountRepository.findByIdIn(accountIds).associateBy { it.id }
        } else emptyMap()

        return MobilePromotionListResponse(
            content = promotionPage.content.map { promotion ->
                val accountName = accountMap[promotion.account!!.id]?.name
                val myScheduleDate = if (isWoman) {
                    promotionEmployeeRepository.findMinScheduleDateByPromotionIdAndEmployeeId(
                        promotion.id, employee.id
                    )
                } else null

                MobilePromotionListItem.from(
                    promotion = promotion,
                    accountName = accountName,
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

        val isWoman = employee.role == AppAuthority.WOMAN

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

        val account = promotion.account
        val product = promotion.primaryProductId?.let { productRepository.findById(it).orElse(null) }

        // 조원 목록 + 사원명 매핑 (fetchJoin으로 N+1 방지)
        val employees = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId)

        val employeeItems = employees.map { emp ->
            MobilePromotionEmployeeItem.from(emp, emp.employee?.name, employee.id)
        }

        return MobilePromotionDetailResponse.from(
            promotion = promotion,
            accountName = account?.name,
            primaryProductName = product?.name,
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
