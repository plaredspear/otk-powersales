package com.otoki.internal.service

import com.otoki.internal.dto.response.OrderCancelResponse
import com.otoki.internal.dto.response.OrderDetailResponse
import com.otoki.internal.dto.response.OrderSummaryResponse
import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.exception.*
import com.otoki.internal.repository.OrderItemRepository
import com.otoki.internal.repository.OrderProcessingRecordRepository
import com.otoki.internal.repository.OrderRejectionRepository
import com.otoki.internal.repository.OrderRepository
import com.otoki.internal.repository.UserRepository
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
    private val orderItemRepository: OrderItemRepository,
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

    /**
     * 주문 상세 조회
     *
     * @param userId 로그인 사용자 ID
     * @param orderId 주문 ID
     * @return 주문 상세 응답
     * @throws OrderNotFoundException 주문이 존재하지 않는 경우
     * @throws ForbiddenOrderAccessException 다른 사용자의 주문에 접근한 경우
     */
    fun getOrderDetail(userId: Long, orderId: Long): OrderDetailResponse {
        // 1. 주문 조회
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException() }

        // 2. 사용자 권한 검증
        if (order.user.id != userId) {
            throw ForbiddenOrderAccessException()
        }

        // 3. 마감 여부 계산
        val isClosed = calculateIsClosed(order.deliveryDate, order.clientDeadlineTime)

        // 4. 관련 데이터 조회
        val items = orderItemRepository.findByOrderId(orderId)
        val processingRecords = orderProcessingRecordRepository.findByOrderId(orderId)
        val rejections = orderRejectionRepository.findByOrderId(orderId)

        // 5. DTO 변환 및 반환
        return OrderDetailResponse.from(
            order = order,
            items = items,
            isClosed = isClosed,
            processingRecords = processingRecords,
            rejections = rejections
        )
    }

    /**
     * 주문 재전송
     *
     * @param userId 로그인 사용자 ID
     * @param orderId 주문 ID
     * @throws OrderNotFoundException 주문이 존재하지 않는 경우
     * @throws ForbiddenOrderAccessException 다른 사용자의 주문에 접근한 경우
     * @throws OrderAlreadyClosedException 마감된 주문인 경우
     * @throws InvalidOrderStatusException 전송실패 상태가 아닌 경우
     */
    @Transactional
    fun resendOrder(userId: Long, orderId: Long) {
        // 1. 주문 조회
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException() }

        // 2. 사용자 권한 검증
        if (order.user.id != userId) {
            throw ForbiddenOrderAccessException()
        }

        // 3. 마감 여부 검증
        val isClosed = calculateIsClosed(order.deliveryDate, order.clientDeadlineTime)
        if (isClosed) {
            throw OrderAlreadyClosedException("마감된 주문은 재전송할 수 없습니다")
        }

        // 4. 승인상태 검증
        if (order.approvalStatus != ApprovalStatus.SEND_FAILED) {
            throw InvalidOrderStatusException()
        }

        // 5. 상태 변경 (SEND_FAILED -> RESEND)
        order.approvalStatus = ApprovalStatus.RESEND
        orderRepository.save(order)
    }

    /**
     * 주문 취소
     *
     * 선택한 제품들의 주문을 취소한다.
     *
     * @param userId 로그인 사용자 ID
     * @param orderId 주문 ID
     * @param productCodes 취소할 제품코드 목록
     * @return 취소 결과 (취소된 제품 수, 제품코드 목록)
     * @throws OrderNotFoundException 주문이 존재하지 않는 경우
     * @throws ForbiddenOrderAccessException 다른 사용자의 주문에 접근한 경우
     * @throws OrderAlreadyClosedException 마감된 주문인 경우
     * @throws ProductNotInOrderException 요청한 제품코드가 해당 주문에 없는 경우
     * @throws AlreadyCancelledException 이미 취소된 제품이 포함된 경우
     */
    @Transactional
    fun cancelOrder(userId: Long, orderId: Long, productCodes: List<String>): OrderCancelResponse {
        // 1. 주문 조회
        val order = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException() }

        // 2. 사용자 권한 검증 (IDOR 방지)
        if (order.user.id != userId) {
            throw ForbiddenOrderAccessException()
        }

        // 3. 마감 여부 검증
        val isClosed = calculateIsClosed(order.deliveryDate, order.clientDeadlineTime)
        if (isClosed) {
            throw OrderAlreadyClosedException("마감된 주문은 취소할 수 없습니다")
        }

        // 4. 해당 주문의 아이템 조회
        val orderItems = orderItemRepository.findByOrderId(orderId)
        val orderItemsByProductCode = orderItems.associateBy { it.productCode }

        // 5. 제품 유효성 검증 — 해당 주문에 포함되지 않은 제품코드 확인
        val notInOrder = productCodes.filter { it !in orderItemsByProductCode }
        if (notInOrder.isNotEmpty()) {
            throw ProductNotInOrderException(notInOrder)
        }

        // 6. 이미 취소된 제품 확인
        val alreadyCancelled = productCodes.filter { orderItemsByProductCode[it]!!.isCancelled }
        if (alreadyCancelled.isNotEmpty()) {
            throw AlreadyCancelledException(alreadyCancelled)
        }

        // 7. 취소 요청자 사번 조회
        val user = userRepository.findById(userId)
            .orElseThrow { ForbiddenOrderAccessException() }

        // 8. 취소 처리
        val cancelledItems = productCodes.map { code ->
            val item = orderItemsByProductCode[code]!!
            item.cancel(user.employeeId)
            item
        }

        // 9. 저장
        orderItemRepository.saveAll(cancelledItems)

        // 10. 응답 구성
        return OrderCancelResponse(
            cancelledCount = cancelledItems.size,
            cancelledProductCodes = cancelledItems.map { it.productCode }
        )
    }

    /**
     * 마감 여부를 계산한다.
     *
     * 규칙:
     * 1. 납기일 이전(오늘이 납기일보다 이전) → isClosed=false
     * 2. 납기일 당일: 최초 마감시간 20분 전까지 → isClosed=false, 그 이후 → isClosed=true
     * 3. 납기일 다음날 이후 → isClosed=true
     * 4. clientDeadlineTime이 null이면 납기일 기준으로만 판단 (당일은 미마감)
     */
    internal fun calculateIsClosed(deliveryDate: LocalDate, clientDeadlineTime: String?): Boolean {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()

        // 납기일 이전: 미마감
        if (today.isBefore(deliveryDate)) {
            return false
        }

        // 납기일 이후(다음날~): 마감
        if (today.isAfter(deliveryDate)) {
            return true
        }

        // 납기일 당일: 마감시간 기준 판단
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
