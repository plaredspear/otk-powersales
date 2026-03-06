/* Order 모듈 전체 비활성화 — DB 테이블 미존재
package com.otoki.internal.order.service

import com.otoki.internal.order.dto.response.OrderSummaryResponse
import com.otoki.internal.order.entity.ApprovalStatus
import com.otoki.internal.order.exception.*
import com.otoki.internal.order.repository.OrderProcessingRecordRepository
import com.otoki.internal.order.repository.OrderRejectionRepository
import com.otoki.internal.order.repository.OrderRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 주문 Service
 */
@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderProcessingRecordRepository: OrderProcessingRecordRepository,
    private val orderRejectionRepository: OrderRejectionRepository,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_SORT_BY = "orderDate"
        private const val DEFAULT_SORT_DIR = "DESC"

        /** 마감시간 20분 전부터 마감 처리 */
        private const val DEADLINE_OFFSET_MINUTES = 20L

        private val VALID_SORT_FIELDS = setOf("orderDate", "deliveryDate", "totalAmount")
        private val VALID_SORT_DIRS = setOf("ASC", "DESC")
        private val VALID_STATUSES = ApprovalStatus.entries.map { it.name }.toSet()
    }

    /**
     * 내 주문 목록 조회
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

    // TODO: Spec #XXX에서 활성화 — getOrderDetail
    // TODO: Spec #XXX에서 활성화 — resendOrder
    // TODO: Spec #XXX에서 활성화 — cancelOrder

    /**
     * 마감 여부를 계산한다.
     */
    internal fun calculateIsClosed(deliveryDate: LocalDate, clientDeadlineTime: String?): Boolean {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()

        if (today.isBefore(deliveryDate)) {
            return false
        }

        if (today.isAfter(deliveryDate)) {
            return true
        }

        if (clientDeadlineTime == null) {
            return false
        }

        return try {
            val deadlineTime = LocalTime.parse(clientDeadlineTime)
            val cutoffTime = deadlineTime.minusMinutes(DEADLINE_OFFSET_MINUTES)
            !now.toLocalTime().isBefore(cutoffTime)
        } catch (e: Exception) {
            false
        }
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
*/