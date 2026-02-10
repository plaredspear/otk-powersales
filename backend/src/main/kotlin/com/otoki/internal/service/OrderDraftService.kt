package com.otoki.internal.service

import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.dto.response.DraftSavedResponse
import com.otoki.internal.dto.response.OrderDraftResponse
import com.otoki.internal.entity.OrderDraft
import com.otoki.internal.entity.OrderDraftItem
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.DraftNotFoundException
import com.otoki.internal.exception.InvalidDeliveryDateException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.repository.OrderDraftRepository
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 임시저장 주문서 Service
 */
@Service
@Transactional(readOnly = true)
class OrderDraftService(
    private val orderDraftRepository: OrderDraftRepository,
    private val productRepository: ProductRepository,
    private val storeRepository: StoreRepository,
    private val userRepository: UserRepository
) {

    /**
     * 임시저장 주문서 조회
     *
     * @param userId 로그인 사용자 ID
     * @return 임시저장 주문서 (없으면 null)
     */
    fun getMyDraft(userId: Long): OrderDraftResponse? {
        val draft = orderDraftRepository.findByUserIdWithItems(userId) ?: return null
        return OrderDraftResponse.from(draft)
    }

    /**
     * 주문서 임시저장
     * 기존 임시저장 데이터가 있으면 삭제하고, 새로 생성한다.
     *
     * @param userId 로그인 사용자 ID
     * @param request 임시저장 요청 데이터
     * @return 저장 결과
     */
    @Transactional
    fun saveDraft(userId: Long, request: OrderDraftRequest): DraftSavedResponse {
        val clientId = request.clientId!!
        val deliveryDateStr = request.deliveryDate!!
        val items = request.items!!

        // 1. 납기일 파싱 및 검증
        val deliveryDate = parseDeliveryDate(deliveryDateStr)
        if (!deliveryDate.isAfter(LocalDate.now())) {
            throw InvalidDeliveryDateException()
        }

        // 2. 거래처 확인
        val store = storeRepository.findById(clientId)
            .orElseThrow { ClientNotFoundException() }

        // 3. 사용자 확인
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다") }

        // 4. 제품 목록 일괄 조회
        val productCodes = items.map { it.productCode!! }
        val products = productRepository.findByProductCodeIn(productCodes)
        val productMap = products.associateBy { it.productCode }

        // 5. 존재하지 않는 제품 확인
        val missingProducts = productCodes.filter { it !in productMap }
        if (missingProducts.isNotEmpty()) {
            throw ProductNotFoundException(missingProducts.first())
        }

        // 6. 기존 임시저장 삭제
        orderDraftRepository.deleteByUserId(userId)

        // 7. 금액 계산 및 OrderDraft 생성
        var totalAmount = 0L
        val draftItems = mutableListOf<OrderDraftItem>()

        val draft = OrderDraft(
            user = user,
            store = store,
            deliveryDate = deliveryDate,
            totalAmount = 0 // 아래에서 계산 후 업데이트
        )

        for (itemReq in items) {
            val product = productMap[itemReq.productCode!!]!!
            val boxQty = itemReq.boxQuantity!!
            val pieceQty = itemReq.pieceQuantity!!
            val amount = (boxQty.toLong() * product.piecesPerBox + pieceQty) * product.unitPrice
            totalAmount += amount

            val draftItem = OrderDraftItem(
                orderDraft = draft,
                productCode = product.productCode,
                productName = product.productName,
                boxQuantity = boxQty,
                pieceQuantity = pieceQty,
                unitPrice = product.unitPrice,
                amount = amount,
                piecesPerBox = product.piecesPerBox,
                minOrderUnit = product.minOrderUnit,
                supplyQuantity = product.supplyQuantity,
                dcQuantity = product.dcQuantity
            )
            draftItems.add(draftItem)
        }

        // 8. totalAmount가 있는 새 OrderDraft 생성 (val이므로 재생성)
        val draftWithTotal = OrderDraft(
            user = user,
            store = store,
            deliveryDate = deliveryDate,
            totalAmount = totalAmount
        )
        for (item in draftItems) {
            val itemWithDraft = OrderDraftItem(
                orderDraft = draftWithTotal,
                productCode = item.productCode,
                productName = item.productName,
                boxQuantity = item.boxQuantity,
                pieceQuantity = item.pieceQuantity,
                unitPrice = item.unitPrice,
                amount = item.amount,
                piecesPerBox = item.piecesPerBox,
                minOrderUnit = item.minOrderUnit,
                supplyQuantity = item.supplyQuantity,
                dcQuantity = item.dcQuantity
            )
            draftWithTotal.items.add(itemWithDraft)
        }

        val saved = orderDraftRepository.save(draftWithTotal)

        return DraftSavedResponse(
            savedAt = saved.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    /**
     * 임시저장 주문서 삭제
     *
     * @param userId 로그인 사용자 ID
     */
    @Transactional
    fun deleteDraft(userId: Long) {
        val draft = orderDraftRepository.findByUserIdWithItems(userId)
            ?: throw DraftNotFoundException()
        orderDraftRepository.delete(draft)
    }

    private fun parseDeliveryDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            throw InvalidDeliveryDateException()
        }
    }
}
