/*
package com.otoki.internal.service

import com.otoki.internal.dto.response.ClientOrderDetailResponse
import com.otoki.internal.dto.response.ClientOrderItemResponse
import com.otoki.internal.dto.response.ClientOrderSummaryResponse
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.ForbiddenClientAccessException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.OrderNotFoundException
import com.otoki.internal.repository.OrderProcessingRecordRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.StoreScheduleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

/ **
 * 거래처별 주문 Service (F28)
 * 특정 거래처에 대한 모든 주문(내 주문 + 다른 영업사원의 주문)을 조회한다.
 * /
@Service
@Transactional(readOnly = true)
class ClientOrderService(
    private val orderProcessingRecordRepository: OrderProcessingRecordRepository,
    private val storeRepository: StoreRepository,
    private val storeScheduleRepository: StoreScheduleRepository
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    / **
     * 거래처별 주문 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @param clientId 거래처 ID (필수)
     * @param deliveryDate 납기일 (기본: 오늘)
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20, 최대: 100)
     * @return 페이지네이션된 거래처별 주문 목록
     * /
    fun getClientOrders(
        userId: Long,
        clientId: Long,
        deliveryDate: LocalDate?,
        page: Int?,
        size: Int?
    ): Page<ClientOrderSummaryResponse> {
        // 1. 파라미터 검증
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE
        validatePagination(resolvedPage, resolvedSize)

        val resolvedDeliveryDate = deliveryDate ?: LocalDate.now()

        // 2. 거래처 존재 여부 확인
        if (!storeRepository.existsById(clientId)) {
            throw ClientNotFoundException()
        }

        // 3. 거래처 접근 권한 검증 (담당 거래처 여부)
        validateClientAccess(userId, clientId)

        // 4. SAP 주문번호별 그룹핑 조회
        val summaries = orderProcessingRecordRepository.findClientOrderSummaries(
            storeId = clientId,
            deliveryDate = resolvedDeliveryDate
        )

        // 5. DTO 변환
        val allItems = summaries.map { row ->
            ClientOrderSummaryResponse(
                sapOrderNumber = row[0] as String,
                clientId = row[1] as Long,
                clientName = row[2] as String,
                totalAmount = (row[3] as Number).toLong()
            )
        }

        // 6. 수동 페이지네이션 적용
        val pageable = PageRequest.of(resolvedPage, resolvedSize)
        val start = (resolvedPage * resolvedSize).coerceAtMost(allItems.size)
        val end = ((resolvedPage + 1) * resolvedSize).coerceAtMost(allItems.size)
        val pageContent = allItems.subList(start, end)

        return PageImpl(pageContent, pageable, allItems.size.toLong())
    }

    / **
     * 거래처별 주문 상세 조회
     *
     * @param userId 로그인 사용자 ID
     * @param sapOrderNumber SAP 주문번호
     * @return 주문 상세 + 제품 목록
     * @throws OrderNotFoundException SAP 주문번호에 해당하는 주문이 없는 경우
     * @throws ForbiddenClientAccessException 해당 거래처에 접근 권한이 없는 경우
     * /
    fun getClientOrderDetail(
        userId: Long,
        sapOrderNumber: String
    ): ClientOrderDetailResponse {
        // 1. SAP 주문번호로 처리 기록 조회
        val records = orderProcessingRecordRepository.findBySapOrderNumber(sapOrderNumber)
        if (records.isEmpty()) {
            throw OrderNotFoundException()
        }

        // 2. 첫 번째 레코드에서 주문 정보 추출 (같은 SAP 번호는 같은 주문)
        val firstRecord = records.first()
        val order = firstRecord.order
        val store = order.store

        // 3. 거래처 접근 권한 검증
        validateClientAccess(userId, store.id)

        // 4. 제품 목록 구성
        val orderedItems = records.map { record ->
            ClientOrderItemResponse(
                productCode = record.productCode,
                productName = record.productName,
                deliveredQuantity = record.deliveredQuantity,
                deliveryStatus = record.deliveryStatus.name
            )
        }

        // 5. 응답 구성
        return ClientOrderDetailResponse(
            sapOrderNumber = sapOrderNumber,
            clientId = store.id,
            clientName = store.storeName,
            clientDeadlineTime = order.clientDeadlineTime,
            orderDate = order.orderDate.toString(),
            deliveryDate = order.deliveryDate.toString(),
            totalApprovedAmount = order.totalApprovedAmount,
            orderedItemCount = orderedItems.size,
            orderedItems = orderedItems
        )
    }

    / **
     * 거래처 접근 권한 검증
     * 로그인한 사용자의 당월 담당 거래처 목록에 해당 clientId가 포함되어 있어야 한다.
     * /
    private fun validateClientAccess(userId: Long, clientId: Long) {
        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val userStoreIds = storeScheduleRepository
            .findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate)

        if (clientId !in userStoreIds) {
            throw ForbiddenClientAccessException()
        }
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
