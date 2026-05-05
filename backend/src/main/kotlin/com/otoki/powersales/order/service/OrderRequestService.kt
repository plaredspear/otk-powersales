package com.otoki.powersales.order.service

import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.order.dto.response.OrderRequestSummaryResponse
import com.otoki.powersales.order.entity.OrderRequestStatus
import com.otoki.powersales.order.exception.InvalidDateRangeException
import com.otoki.powersales.order.exception.InvalidOrderParameterException
import com.otoki.powersales.order.exception.OrderDateRangeTooWideException
import com.otoki.powersales.order.repository.OrderRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

@Service
@Transactional(readOnly = true)
class OrderRequestService(
    private val orderRequestRepository: OrderRequestRepository,
    private val clock: Clock = Clock.system(TimeZones.SEOUL_ZONE),
) {

    companion object {
        private const val DEFAULT_SORT_BY = "orderDate"
        private const val DEFAULT_SORT_DIR = "DESC"

        /** 마감 시각 20분 전부터 마감 처리 */
        private const val DEADLINE_OFFSET_MINUTES = 20L

        /** 응답 라인 수 상한 — 초과 시 truncated=true */
        private const val MAX_ROWS = 2000

        /** 기간 max 일 (레거시 list.jsp 동등) */
        private const val MAX_DATE_RANGE_DAYS = 7L

        private val VALID_SORT_FIELDS = setOf("orderDate", "deliveryDate", "totalAmount")
        private val VALID_SORT_DIRS = setOf("ASC", "DESC")

        private val log = LoggerFactory.getLogger(OrderRequestService::class.java)
    }

    fun getMyOrderRequests(
        userId: Long,
        accountId: Long?,
        status: String?,
        deliveryDateFrom: LocalDate?,
        deliveryDateTo: LocalDate?,
        sortBy: String?,
        sortDir: String?,
    ): OrderRequestListResponse {
        if (deliveryDateFrom == null || deliveryDateTo == null) {
            throw InvalidOrderParameterException("deliveryDateFrom 과 deliveryDateTo 는 필수입니다.")
        }
        if (deliveryDateTo.isBefore(deliveryDateFrom)) {
            throw InvalidDateRangeException()
        }
        val rangeDays = deliveryDateFrom.until(deliveryDateTo, java.time.temporal.ChronoUnit.DAYS)
        if (rangeDays > MAX_DATE_RANGE_DAYS) {
            throw OrderDateRangeTooWideException()
        }

        val resolvedSortBy = (sortBy ?: DEFAULT_SORT_BY).also(::validateSortBy)
        val resolvedSortDir = (sortDir ?: DEFAULT_SORT_DIR).also(::validateSortDir)
        val parsedStatus = status?.let(::parseStatus)
        if (accountId != null && accountId < 1) {
            throw InvalidOrderParameterException("clientId 는 1 이상이어야 합니다.")
        }

        // limit + 1 만큼 조회하여 truncated 여부 판단
        val rows = orderRequestRepository.findMyOrderRequests(
            employeeId = userId,
            accountId = accountId,
            status = parsedStatus,
            deliveryDateFrom = deliveryDateFrom,
            deliveryDateTo = deliveryDateTo,
            sortBy = resolvedSortBy,
            sortDir = resolvedSortDir,
            limit = MAX_ROWS + 1,
        )
        val truncated = rows.size > MAX_ROWS
        val effective = if (truncated) rows.subList(0, MAX_ROWS) else rows
        if (truncated) {
            log.warn("[order-request] 응답 라인 수 상한 도달: userId={}, total={}", userId, rows.size)
        }

        val items = effective.map { OrderRequestSummaryResponse.from(it, calculateIsClosed(it.deliveryDate, it.clientDeadlineTime)) }
        val fetchedAt = OffsetDateTime.now(clock)

        return OrderRequestListResponse(
            items = items,
            total = items.size,
            truncated = truncated,
            fetchedAt = fetchedAt,
        )
    }

    /**
     * 마감 여부 계산.
     * - 오늘 < deliveryDate → false
     * - 오늘 > deliveryDate → true
     * - 오늘 == deliveryDate:
     *     - clientDeadlineTime null → false
     *     - 현재 시각 ≥ deadline - 20분 → true, 그 외 false
     */
    internal fun calculateIsClosed(deliveryDate: LocalDate, clientDeadlineTime: String?): Boolean {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()

        if (today.isBefore(deliveryDate)) return false
        if (today.isAfter(deliveryDate)) return true
        if (clientDeadlineTime.isNullOrBlank()) return false

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
                "정렬 기준은 ${VALID_SORT_FIELDS.joinToString(", ")} 중 하나여야 합니다.",
            )
        }
    }

    private fun validateSortDir(sortDir: String) {
        if (sortDir !in VALID_SORT_DIRS) {
            throw InvalidOrderParameterException("정렬 방향은 ASC 또는 DESC 여야 합니다.")
        }
    }

    private fun parseStatus(status: String): OrderRequestStatus {
        return try {
            OrderRequestStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            throw InvalidOrderParameterException(
                "승인상태는 ${OrderRequestStatus.entries.joinToString(", ") { it.name }} 중 하나여야 합니다.",
            )
        }
    }
}
