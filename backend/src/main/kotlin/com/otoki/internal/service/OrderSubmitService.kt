/*
package com.otoki.internal.service

import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.dto.response.InvalidItemResponse
import com.otoki.internal.dto.response.OrderSubmitResponse
import com.otoki.internal.dto.response.ValidationResultResponse
import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.entity.Order
import com.otoki.internal.entity.OrderItem
import com.otoki.internal.entity.Product
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.InvalidDeliveryDateException
import com.otoki.internal.exception.OrderValidationFailedException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.integration.SapOrderClient
import com.otoki.internal.repository.OrderDraftRepository
import com.otoki.internal.repository.OrderItemRepository
import com.otoki.internal.repository.OrderRepository
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/ **
 * 주문서 제출/유효성 검증 Service
 * /
@Service
@Transactional(readOnly = true)
class OrderSubmitService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderDraftRepository: OrderDraftRepository,
    private val productRepository: ProductRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository,
    private val sapOrderClient: SapOrderClient
) {

    companion object {
        private const val ORDER_NUMBER_PREFIX = "OP"
        private const val ORDER_NUMBER_LENGTH = 8
    }

    / **
     * 주문서 유효성 체크
     *
     * @param userId 로그인 사용자 ID
     * @param request 주문서 요청 데이터
     * @return 유효성 검증 결과
     * /
    fun validateOrder(userId: Long, request: OrderDraftRequest): ValidationResultResponse {
        val clientId = request.clientId!!
        val deliveryDateStr = request.deliveryDate!!
        val items = request.items!!

        // 1. 납기일 검증
        parseDeliveryDate(deliveryDateStr)

        // 2. 거래처 존재 확인
        storeRepository.findById(clientId)
            .orElseThrow { ClientNotFoundException() }

        // 3. 제품 일괄 조회
        val productCodes = items.map { it.productCode!! }
        val products = productRepository.findByProductCodeIn(productCodes)
        val productMap = products.associateBy { it.productCode }

        // 4. 존재하지 않는 제품 확인
        val missingProducts = productCodes.filter { it !in productMap }
        if (missingProducts.isNotEmpty()) {
            throw ProductNotFoundException(missingProducts.first())
        }

        // 5. 제품별 유효성 검증
        val invalidItems = mutableListOf<InvalidItemResponse>()

        for (itemReq in items) {
            val product = productMap[itemReq.productCode!!]!!
            val boxQty = itemReq.boxQuantity!!
            val pieceQty = itemReq.pieceQuantity!!
            val totalQuantity = boxQty.toLong() * product.piecesPerBox + pieceQty

            val errors = validateItem(totalQuantity, boxQty, pieceQty, product)

            if (errors.isNotEmpty()) {
                invalidItems.add(
                    InvalidItemResponse(
                        productCode = product.productCode,
                        productName = product.productName,
                        boxQuantity = boxQty,
                        pieceQuantity = pieceQty,
                        piecesPerBox = product.piecesPerBox,
                        minOrderUnit = product.minOrderUnit,
                        supplyQuantity = product.supplyQuantity,
                        dcQuantity = product.dcQuantity,
                        validationErrors = errors
                    )
                )
            }
        }

        return if (invalidItems.isEmpty()) {
            ValidationResultResponse(isValid = true, invalidItems = emptyList())
        } else {
            ValidationResultResponse(isValid = false, invalidItems = invalidItems)
        }
    }

    / **
     * 주문서 제출 (승인요청)
     *
     * @param userId 로그인 사용자 ID
     * @param request 주문서 요청 데이터
     * @return 주문 제출 결과
     * /
    @Transactional
    fun submitOrder(userId: Long, request: OrderDraftRequest): OrderSubmitResponse {
        val clientId = request.clientId!!
        val deliveryDateStr = request.deliveryDate!!
        val items = request.items!!

        // 1. 납기일 파싱
        val deliveryDate = parseDeliveryDate(deliveryDateStr)
        if (!deliveryDate.isAfter(LocalDate.now())) {
            throw InvalidDeliveryDateException()
        }

        // 2. 유효성 검증
        val validationResult = validateOrder(userId, request)
        if (!validationResult.isValid) {
            throw OrderValidationFailedException()
        }

        // 3. 거래처 조회
        val store = storeRepository.findById(clientId)
            .orElseThrow { ClientNotFoundException() }

        // 4. 사용자 조회
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다") }

        // 5. 제품 일괄 조회
        val productCodes = items.map { it.productCode!! }
        val products = productRepository.findByProductCodeIn(productCodes)
        val productMap = products.associateBy { it.productCode }

        // 6. 주문번호 생성
        val orderRequestNumber = generateOrderNumber()

        // 7. 금액 계산 및 Order 생성
        var totalAmount = 0L
        val order = Order(
            orderRequestNumber = orderRequestNumber,
            user = user,
            store = store,
            orderDate = LocalDate.now(),
            deliveryDate = deliveryDate,
            approvalStatus = ApprovalStatus.PENDING
        )

        val orderItems = mutableListOf<OrderItem>()
        for (itemReq in items) {
            val product = productMap[itemReq.productCode!!]!!
            val boxQty = itemReq.boxQuantity!!
            val pieceQty = itemReq.pieceQuantity!!
            val amount = (boxQty.toLong() * product.piecesPerBox + pieceQty) * product.unitPrice
            totalAmount += amount

            orderItems.add(
                OrderItem(
                    order = order,
                    productCode = product.productCode,
                    productName = product.productName,
                    quantityBoxes = boxQty.toDouble(),
                    quantityPieces = pieceQty,
                    unitPrice = product.unitPrice,
                    amount = amount,
                    piecesPerBox = product.piecesPerBox,
                    minOrderUnit = product.minOrderUnit,
                    supplyQuantity = product.supplyQuantity,
                    dcQuantity = product.dcQuantity
                )
            )
        }

        // 8. Order에 totalAmount 설정 (val이므로 새 인스턴스 생성)
        val orderWithTotal = Order(
            orderRequestNumber = orderRequestNumber,
            user = user,
            store = store,
            orderDate = LocalDate.now(),
            deliveryDate = deliveryDate,
            totalAmount = totalAmount,
            approvalStatus = ApprovalStatus.PENDING
        )

        val savedOrder = orderRepository.save(orderWithTotal)

        // 9. OrderItem 저장
        val savedItems = orderItems.map { item ->
            OrderItem(
                order = savedOrder,
                productCode = item.productCode,
                productName = item.productName,
                quantityBoxes = item.quantityBoxes,
                quantityPieces = item.quantityPieces,
                unitPrice = item.unitPrice,
                amount = item.amount,
                piecesPerBox = item.piecesPerBox,
                minOrderUnit = item.minOrderUnit,
                supplyQuantity = item.supplyQuantity,
                dcQuantity = item.dcQuantity
            )
        }
        orderItemRepository.saveAll(savedItems)

        // 10. SAP 전송
        var failureReason: String? = null
        try {
            val sapResult = sapOrderClient.sendOrder(savedOrder)
            if (sapResult.success) {
                savedOrder.approvalStatus = ApprovalStatus.APPROVED
            } else {
                savedOrder.approvalStatus = ApprovalStatus.SEND_FAILED
                failureReason = sapResult.failureReason
            }
        } catch (e: Exception) {
            savedOrder.approvalStatus = ApprovalStatus.SEND_FAILED
            failureReason = e.message ?: "SAP 전송 중 오류가 발생했습니다"
        }
        orderRepository.save(savedOrder)

        // 11. 임시저장 삭제 (있으면)
        orderDraftRepository.deleteByUserId(userId)

        // 12. 응답 구성
        return OrderSubmitResponse(
            orderId = savedOrder.id,
            orderRequestNumber = savedOrder.orderRequestNumber,
            approvalStatus = savedOrder.approvalStatus.name,
            totalAmount = totalAmount,
            submittedAt = savedOrder.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            failureReason = failureReason
        )
    }

    / **
     * 개별 제품 유효성 검증
     * /
    private fun validateItem(totalQuantity: Long, boxQty: Int, pieceQty: Int, product: Product): List<String> {
        val errors = mutableListOf<String>()

        // 1. 수량 입력 여부
        if (boxQty == 0 && pieceQty == 0) {
            errors.add("수량을 입력해주세요")
        }

        // 2. 최소주문단위
        if (totalQuantity > 0 && totalQuantity < product.minOrderUnit) {
            errors.add("수량이 최소주문단위(${product.minOrderUnit}개)보다 작습니다")
        }

        // 3. 공급수량 초과
        if (product.supplyQuantity > 0 && totalQuantity > product.supplyQuantity) {
            errors.add("수량이 공급수량(${product.supplyQuantity}개)을 초과했습니다")
        }

        // 4. DC수량 초과
        if (product.dcQuantity > 0 && totalQuantity > product.dcQuantity) {
            errors.add("수량이 DC수량(${product.dcQuantity}개)을 초과했습니다")
        }

        return errors
    }

    / **
     * 주문 요청번호 생성 (OP + 8자리 숫자)
     * /
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis() % 100_000_000
        return "$ORDER_NUMBER_PREFIX${timestamp.toString().padStart(ORDER_NUMBER_LENGTH, '0')}"
    }

    private fun parseDeliveryDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            throw InvalidDeliveryDateException()
        }
    }
}
*/
