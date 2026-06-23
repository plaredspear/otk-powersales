package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.response.OrderHistoryGroupResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderHistoryProductResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestSummaryResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderedItemResponse
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.InvalidDateRangeException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.exception.OrderDateRangeTooWideException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class OrderRequestService(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
    private val orderRequestDetailSapSender: OrderRequestDetailSapSender,
    private val orderRequestDetailMapper: OrderRequestDetailMapper,
    private val productRepository: ProductRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
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
        val rangeDays = deliveryDateFrom.until(deliveryDateTo, ChronoUnit.DAYS)
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
     * 거래처 주문이력(제품 선택용) 조회 — 레거시 SF `OrderHistory`(IF_REST_MOBILE_OrderHistory) 정합.
     *
     * 본인이 해당 거래처(account.externalKey = accountCode)에 등록한 주문요청의 제품을
     * 주문일(orderDate) 범위로 조회하여 주문일별로 그룹핑한다.
     * 레거시와 동일하게 종료일은 해당일 전체 포함(EndDate +1일), 그룹 내 제품코드는 중복제거.
     *
     * @param userId JWT 사용자 ID (= employee.id)
     * @param accountCode 거래처 SAP 코드 (Account.externalKey)
     * @param startDate 주문일 시작
     * @param endDate 주문일 종료 (inclusive)
     * @return 주문일 내림차순 그룹 목록
     */
    fun getAccountOrderHistory(
        userId: Long,
        accountCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<OrderHistoryGroupResponse> {
        if (endDate.isBefore(startDate)) {
            throw InvalidDateRangeException()
        }

        val rows = orderRequestRepository.findOrderHistory(
            employeeId = userId,
            accountCode = accountCode,
            orderDateFrom = startDate.atStartOfDay(),
            orderDateToExclusive = endDate.plusDays(1).atStartOfDay(),
        )

        return rows
            .filter { it.orderDate != null && !it.productCode.isNullOrBlank() }
            .groupBy { it.orderDate!!.toLocalDate() }
            .toSortedMap(reverseOrder())
            .map { (date, groupRows) ->
                val products = groupRows
                    .distinctBy { it.productCode }
                    .map { OrderHistoryProductResponse(it.productCode!!, it.productName) }
                OrderHistoryGroupResponse(orderDate = date.toString(), products = products)
            }
    }

    /**
     * 본인 주문요청 상세 조회 (Spec #595 P1-B).
     *
     * 처리 흐름:
     * 1. `OrderRequest` 조회 → 미존재 시 `OrderNotFoundException`.
     * 2. 권한 게이트 — `OrderRequest.employee.id != userId` 시 `ForbiddenOrderAccessException` (레거시 강화).
     * 3. `OrderRequestProduct` 라인 조회 (CRM).
     * 4. 마감 여부 계산.
     * 5. SAP 동기 호출 — 마감 여부 무관 항상 수행.
     * 6. SAP 응답 + CRM 라인 결합 → `orderProcessingStatusList[]` + `rejectedItems[]` 빌드.
     * 7. 마감 전(`isClosed = false`) 시 `orderProcessingStatusList = null` 강제 (Q6 — 레거시 동등).
     */
    fun getOrderRequestDetail(orderRequestId: Long, userId: Long): OrderRequestDetailResponse {
        if (orderRequestId < 1) {
            throw InvalidOrderParameterException("orderRequestId 는 1 이상이어야 합니다.")
        }

        val orderRequest = orderRequestRepository.findByIdOrNull(orderRequestId)
            ?: throw OrderNotFoundException()

        if (orderRequest.employee!!.id != userId) {
            throw ForbiddenOrderAccessException()
        }

        val crmProducts = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)
        val crmProductsByCode = crmProducts.associateBy { it.productCode }

        val isClosed = calculateIsClosed(orderRequest.deliveryDate, orderRequest.clientDeadlineTime)

        val sapLines = orderRequestDetailSapSender.fetchDetail(orderRequest.orderRequestNumber)

        val mapped = if (sapLines == null) {
            null
        } else {
            orderRequestDetailMapper.map(
                requestNumber = orderRequest.orderRequestNumber,
                sapLines = sapLines,
                crmProductsByCode = crmProductsByCode,
            )
        }

        val processingGroups = mapped?.processingGroups?.takeIf { it.isNotEmpty() }
        val rejectedItems = mapped?.rejectedItems?.takeIf { it.isNotEmpty() }
        val outOfStockReasons = mapped?.outOfStockReasons.orEmpty()

        // 마감 전 — 그룹 응답 매핑 생략 (Q6, 레거시 동등). SAP 호출은 이미 수행.
        val finalProcessingGroups = if (isClosed) processingGroups else null

        // 제품명은 product_code 로 제품마스터에서 조회 (레거시 CRM_ProductName = ProductId__r.Name 동등).
        // 주문 라인의 product FK 가 비어 있어도 코드 기준으로 이름을 채운다.
        val productNamesByCode = productRepository
            .findByProductCodeIn(crmProducts.map { it.productCode })
            .associate { it.productCode to it.name }

        // 결품(SAP DefaultReason) 제품은 "주문한 제품" 리스트에 결품 플래그로 표시 (레거시 view.jsp:414 동등).
        val orderedItems = crmProducts.map {
            OrderedItemResponse.from(
                it,
                productName = productNamesByCode[it.productCode],
                outOfStockReason = outOfStockReasons[it.productCode],
            )
        }

        return OrderRequestDetailResponse.of(
            orderRequest = orderRequest,
            isClosed = isClosed,
            orderedItems = orderedItems,
            orderProcessingStatusList = finalProcessingGroups,
            rejectedItems = rejectedItems,
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
