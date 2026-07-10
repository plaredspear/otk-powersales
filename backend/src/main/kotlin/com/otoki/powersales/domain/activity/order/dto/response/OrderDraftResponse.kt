package com.otoki.powersales.domain.activity.order.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 주문 임시저장 등록 응답 — `POST /api/v1/mobile/orders/draft` (Spec #596).
 */
data class OrderDraftSaveResponse(
    val draftId: Long,
    val savedAt: LocalDateTime,
)

/**
 * 주문 임시저장 조회 응답 — `GET /api/v1/mobile/orders/draft` (Spec #596).
 *
 * 거래처명/external_key/`totalAmount` 헤더 + 라인 List. 납기일(`deliveryDate`)은
 * 레거시 `tmp_orderdate` 정합으로 `tmp_order.order_date` 에서 복원 (없으면 null).
 */
data class OrderDraftDetailResponse(
    val draftId: Long,
    val accountId: Long,
    val accountName: String,
    val accountExternalKey: String?,
    val deliveryDate: LocalDate?,
    val totalAmount: Long,
    val savedAt: LocalDateTime,
    val lines: List<OrderDraftLineResponse>,
)

data class OrderDraftLineResponse(
    val lineNumber: Int,
    val productCode: String,
    val productName: String?,
    val unit: String,
    val quantity: BigDecimal,
    val quantityPieces: Int?,
    val quantityBoxes: BigDecimal?,
    /**
     * 1박스당 입수(boxReceivingQuantity). 복원 시 박스→낱개 환산·소계 재계산에 필수.
     * 레거시 `selectTempPrdList` 처럼 저장값이 아니라 제품 마스터에서 재조회한 값이다.
     */
    val boxSize: Int,
    /**
     * 낱개단가(표준단가 + 주세). 레거시 `selectTempPrdList` 정합 — 저장된 임시 단가가 아니라
     * 제품 마스터에서 재조회해 내려준다(가격 변동·구버전/마이그레이션 draft 의 0/누락 단가 보정).
     */
    val unitPrice: BigDecimal?,
    val amount: BigDecimal?,
)
