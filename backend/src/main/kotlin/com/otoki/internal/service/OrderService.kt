package com.otoki.internal.service

import com.otoki.internal.dto.response.OrderSummaryResponse
import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.exception.InvalidDateRangeException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.repository.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 주문 Service
 */
@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_SORT_BY = "orderDate"
        private const val DEFAULT_SORT_DIR = "DESC"

        private val VALID_SORT_FIELDS = setOf("orderDate", "deliveryDate", "totalAmount")
        private val VALID_SORT_DIRS = setOf("ASC", "DESC")
        private val VALID_STATUSES = ApprovalStatus.entries.map { it.name }.toSet()
    }

    /**
     * 내 주문 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @param storeId 거래처 ID (선택)
     * @param status 승인상태 문자열 (선택)
     * @param deliveryDateFrom 납기일 시작 (선택)
     * @param deliveryDateTo 납기일 종료 (선택)
     * @param sortBy 정렬 기준 (기본: orderDate)
     * @param sortDir 정렬 방향 (기본: DESC)
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20)
     * @return 주문 목록 Page
     */
    fun getMyOrders(
        userId: Long,
        storeId: Long?,
        status: String?,
        deliveryDateFrom: LocalDate?,
        deliveryDateTo: LocalDate?,
        sortBy: String?,
        sortDir: String?,
        page: Int?,
        size: Int?
    ): Page<OrderSummaryResponse> {
        // 파라미터 검증
        val resolvedSortBy = sortBy ?: DEFAULT_SORT_BY
        val resolvedSortDir = sortDir ?: DEFAULT_SORT_DIR
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE

        validateSortBy(resolvedSortBy)
        validateSortDir(resolvedSortDir)
        validatePagination(resolvedPage, resolvedSize)

        val approvalStatus = status?.let { validateAndParseStatus(it) }

        if (deliveryDateFrom != null && deliveryDateTo != null) {
            if (deliveryDateTo.isBefore(deliveryDateFrom)) {
                throw InvalidDateRangeException()
            }
        }

        // 정렬 설정
        val direction = Sort.Direction.valueOf(resolvedSortDir)
        val sort = Sort.by(direction, resolvedSortBy)
        val pageable = PageRequest.of(resolvedPage, resolvedSize, sort)

        // 조회
        val orderPage = orderRepository.findByUserIdWithFilters(
            userId = userId,
            storeId = storeId,
            status = approvalStatus,
            deliveryDateFrom = deliveryDateFrom,
            deliveryDateTo = deliveryDateTo,
            pageable = pageable
        )

        return orderPage.map { OrderSummaryResponse.from(it) }
    }

    private fun validateSortBy(sortBy: String) {
        if (sortBy !in VALID_SORT_FIELDS) {
            throw InvalidOrderParameterException(
                "정렬 기준은 ${VALID_SORT_FIELDS.joinToString(", ")} 중 하나여야 합니다"
            )
        }
    }

    private fun validateSortDir(sortDir: String) {
        if (sortDir.uppercase() !in VALID_SORT_DIRS) {
            throw InvalidOrderParameterException(
                "정렬 방향은 ASC 또는 DESC여야 합니다"
            )
        }
    }

    private fun validateAndParseStatus(status: String): ApprovalStatus {
        if (status !in VALID_STATUSES) {
            throw InvalidOrderParameterException(
                "승인상태는 ${VALID_STATUSES.joinToString(", ")} 중 하나여야 합니다"
            )
        }
        return ApprovalStatus.valueOf(status)
    }

    private fun validatePagination(page: Int, size: Int) {
        if (page < 0) {
            throw InvalidOrderParameterException("페이지 번호는 0 이상이어야 합니다")
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw InvalidOrderParameterException("페이지 크기는 1~${MAX_PAGE_SIZE} 범위여야 합니다")
        }
    }
}
