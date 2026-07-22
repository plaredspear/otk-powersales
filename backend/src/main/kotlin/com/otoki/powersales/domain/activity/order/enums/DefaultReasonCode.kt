package com.otoki.powersales.domain.activity.order.enums

/**
 * SAP `DefaultReason`(레거시 `미납사유`) 코드값의 분류/표시 문구 매핑 (Spec #845 P1-B).
 *
 * SAP 상세조회(SD03052) 응답의 `DefaultReason` 은 코드값이다. 코드로 **결품/취소**를 분류하고,
 * 반려사유처럼 **코드+설명** 문구를 생성한다. 매퍼([com.otoki.powersales.domain.activity.order.service.OrderRequestDetailMapper])와
 * 서비스가 공유하는 단일 진실(SoT) — 분류 판정과 표시 문구 생성을 모두 이 컴포넌트가 담당한다.
 *
 * 분류 규칙:
 * - `DefaultReason` 이 비어있으면(null/공백) 분류 대상 아님(정상 라인) → [classify] = null.
 * - 결품 코드셋 = {F1, L1, L2, L3} → [DefaultReasonClassification.OUT_OF_STOCK].
 * - 채워졌으나 결품셋이 아닌 코드(표 정의/미정의 무관) → [DefaultReasonClassification.CANCELLED].
 *
 * 표시 문구 규칙([displayReason]):
 * - 표 정의 코드: `"{코드} {설명}"` (예: `"L1 [물류] 재고부족"`, `"S2 [영업] 고객사정에 의한 취소"`).
 * - 미정의 코드: `"{코드}"` (설명 없이 코드만).
 * - 공백: null.
 *
 * 대소문자·공백은 trim 후 판정한다.
 */
enum class DefaultReasonCode(
    val code: String,
    val classification: DefaultReasonClassification,
    val description: String,
) {
    F1("F1", DefaultReasonClassification.OUT_OF_STOCK, "[공장] 생산불량재고"),
    L1("L1", DefaultReasonClassification.OUT_OF_STOCK, "[물류] 재고부족"),
    L2("L2", DefaultReasonClassification.OUT_OF_STOCK, "[물류] 유통기한 임박품목"),
    L3("L3", DefaultReasonClassification.OUT_OF_STOCK, "[물류] 유통기한 입고기준 미달"),
    L4("L4", DefaultReasonClassification.CANCELLED, "[물류] 긴급주문 사전 미협의"),
    O1("O1", DefaultReasonClassification.CANCELLED, "[기타] 전산정리"),
    S1("S1", DefaultReasonClassification.CANCELLED, "[영업] 주문오류분 취소"),
    S2("S2", DefaultReasonClassification.CANCELLED, "[영업] 고객사정에 의한 취소"),
    S3("S3", DefaultReasonClassification.CANCELLED, "[영업] 영업미배송요청"),
    ;

    companion object {
        private val BY_CODE: Map<String, DefaultReasonCode> = entries.associateBy { it.code }

        /**
         * 코드값을 결품/취소로 분류한다.
         *
         * 공백(null/빈문자)이면 null(정상 라인). 결품셋 코드는 [DefaultReasonClassification.OUT_OF_STOCK],
         * 그 외 채워진 코드(정의/미정의 무관)는 [DefaultReasonClassification.CANCELLED].
         */
        fun classify(rawCode: String?): DefaultReasonClassification? {
            val trimmed = rawCode?.trim()
            if (trimmed.isNullOrEmpty()) return null
            return BY_CODE[trimmed]?.classification ?: DefaultReasonClassification.CANCELLED
        }

        /**
         * 표시 사유 문구를 생성한다.
         *
         * 정의 코드는 `"{코드} {설명}"`, 미정의 코드는 코드만, 공백은 null. 판정은 trim 후 수행한다.
         */
        fun displayReason(rawCode: String?): String? {
            val trimmed = rawCode?.trim()
            if (trimmed.isNullOrEmpty()) return null
            val defined = BY_CODE[trimmed]
            return if (defined != null) "${defined.code} ${defined.description}" else trimmed
        }
    }
}

/** SAP `DefaultReason` 코드 분류 — 결품(재입고 대상) / 취소(라인 취소). */
enum class DefaultReasonClassification {
    OUT_OF_STOCK,
    CANCELLED,
}
