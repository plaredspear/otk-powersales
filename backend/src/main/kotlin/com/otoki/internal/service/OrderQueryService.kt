/*
package com.otoki.internal.service

import com.otoki.internal.dto.response.CreditBalanceResponse
import com.otoki.internal.dto.response.OrderHistoryProductResponse
import com.otoki.internal.dto.response.ProductOrderInfoResponse
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.InvalidOrderDateRangeException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.repository.OrderItemRepository
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.StoreRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/ **
 * 주문 조회 관련 Service
 * - 주문이력 제품 조회
 * - 거래처 여신잔액 조회
 * - 제품 주문정보 조회
 * /
@Service
@Transactional(readOnly = true)
class OrderQueryService(
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val storeRepository: StoreRepository
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_DATE_RANGE_DAYS = 3L
    }

    / **
     * 주문이력 제품 조회
     *
     * 과거 주문 이력에서 제품 목록을 조회합니다. 같은 제품이 여러 번 주문된 경우
     * 중복 제거하고, 마지막 주문일과 주문 횟수를 함께 반환합니다.
     *
     * @param userId 로그인 사용자 ID
     * @param orderDateFrom 주문일 시작 (기본: 오늘-3일)
     * @param orderDateTo 주문일 종료 (기본: 오늘)
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20, 최대: 100)
     * @return 주문이력 제품 페이지
     * /
    fun getOrderHistoryProducts(
        userId: Long,
        orderDateFrom: LocalDate?,
        orderDateTo: LocalDate?,
        page: Int?,
        size: Int?
    ): Page<OrderHistoryProductResponse> {
        val resolvedFrom = orderDateFrom ?: LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS)
        val resolvedTo = orderDateTo ?: LocalDate.now()
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE

        validatePagination(resolvedPage, resolvedSize)

        if (resolvedTo.isBefore(resolvedFrom)) {
            throw InvalidOrderDateRangeException()
        }

        val pageable = PageRequest.of(resolvedPage, resolvedSize)

        // 주문이력 제품 집계 쿼리
        val results = orderItemRepository.findOrderHistoryProducts(userId, resolvedFrom, resolvedTo, pageable)
        val totalCount = orderItemRepository.countOrderHistoryProducts(userId, resolvedFrom, resolvedTo)

        val products = results.map { row ->
            OrderHistoryProductResponse(
                productCode = row[0] as String,
                productName = row[1] as String,
                barcode = (row[2] as? String) ?: "",
                storageType = (row[3] as? String) ?: "",
                categoryMid = row[4] as? String,
                categorySub = row[5] as? String,
                lastOrderDate = (row[6] as LocalDate).format(DateTimeFormatter.ISO_LOCAL_DATE),
                totalOrderCount = row[7] as Long
            )
        }

        return PageImpl(products, pageable, totalCount)
    }

    / **
     * 거래처 여신잔액 조회
     *
     * @param clientId 거래처 ID
     * @return 여신잔액 정보
     * @throws ClientNotFoundException 거래처를 찾을 수 없는 경우
     * /
    fun getClientCreditBalance(clientId: Long): CreditBalanceResponse {
        val store = storeRepository.findById(clientId)
            .orElseThrow { ClientNotFoundException() }

        return CreditBalanceResponse(
            clientId = store.id,
            clientName = store.storeName,
            creditLimit = store.creditLimit,
            usedCredit = store.usedCredit,
            availableCredit = store.creditLimit - store.usedCredit,
            lastUpdatedAt = store.creditUpdatedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    / **
     * 제품 주문정보 조회
     *
     * @param productCode 제품코드
     * @return 제품 주문정보
     * @throws ProductNotFoundException 제품을 찾을 수 없는 경우
     * /
    fun getProductOrderInfo(productCode: String): ProductOrderInfoResponse {
        val product = productRepository.findByProductCode(productCode)
            ?: throw ProductNotFoundException(productCode)

        return ProductOrderInfoResponse(
            productCode = product.productCode,
            productName = product.productName,
            piecesPerBox = product.piecesPerBox,
            minOrderUnit = product.minOrderUnit,
            supplyQuantity = product.supplyQuantity,
            dcQuantity = product.dcQuantity,
            unitPrice = product.unitPrice
        )
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
