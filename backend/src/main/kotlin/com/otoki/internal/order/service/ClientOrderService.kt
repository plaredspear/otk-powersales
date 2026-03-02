package com.otoki.internal.order.service

import com.otoki.internal.order.dto.response.ClientOrderSummaryResponse
import com.otoki.internal.order.exception.ClientNotFoundException
import com.otoki.internal.order.exception.InvalidOrderParameterException
import com.otoki.internal.order.repository.OrderProcessingRecordRepository
import com.otoki.internal.order.repository.StoreRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 거래처별 주문 Service (F28)
 * 특정 거래처에 대한 모든 주문(내 주문 + 다른 영업사원의 주문)을 조회한다.
 */
@Service
@Transactional(readOnly = true)
class ClientOrderService(
    private val orderProcessingRecordRepository: OrderProcessingRecordRepository,
    private val storeRepository: StoreRepository
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * 거래처별 주문 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @param clientId 거래처 ID (필수)
     * @param deliveryDate 납기일 (기본: 오늘)
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20, 최대: 100)
     * @return 페이지네이션된 거래처별 주문 목록
     */
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

        // 3. SAP 주문번호별 그룹핑 조회
        val summaries = orderProcessingRecordRepository.findClientOrderSummaries(
            storeId = clientId,
            deliveryDate = resolvedDeliveryDate
        )

        // 4. DTO 변환
        val allItems = summaries.map { row ->
            ClientOrderSummaryResponse(
                sapOrderNumber = row[0] as String,
                clientId = row[1] as Long,
                clientName = row[2] as String,
                totalAmount = (row[3] as Number).toLong()
            )
        }

        // 5. 수동 페이지네이션 적용
        val pageable = PageRequest.of(resolvedPage, resolvedSize)
        val start = (resolvedPage * resolvedSize).coerceAtMost(allItems.size)
        val end = ((resolvedPage + 1) * resolvedSize).coerceAtMost(allItems.size)
        val pageContent = allItems.subList(start, end)

        return PageImpl(pageContent, pageable, allItems.size.toLong())
    }

    // TODO: Spec #XXX에서 활성화 — getClientOrderDetail

    private fun validatePagination(page: Int, size: Int) {
        if (page < 0) {
            throw InvalidOrderParameterException("페이지 번호는 0 이상이어야 합니다")
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw InvalidOrderParameterException("페이지 크기는 1~${MAX_PAGE_SIZE} 범위여야 합니다")
        }
    }
}
