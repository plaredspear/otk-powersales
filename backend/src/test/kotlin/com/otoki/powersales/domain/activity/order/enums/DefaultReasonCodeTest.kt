package com.otoki.powersales.domain.activity.order.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DefaultReasonCode 테스트 (#845 P1-B)")
class DefaultReasonCodeTest {

    @Test
    @DisplayName("분류-결품 — 코드 L1 → OUT_OF_STOCK + 사유 'L1 [물류] 재고부족'")
    fun classifyOutOfStock() {
        assertThat(DefaultReasonCode.classify("L1")).isEqualTo(DefaultReasonClassification.OUT_OF_STOCK)
        assertThat(DefaultReasonCode.displayReason("L1")).isEqualTo("L1 [물류] 재고부족")
    }

    @Test
    @DisplayName("결품셋 전체(F1/L1/L2/L3) → OUT_OF_STOCK")
    fun outOfStockSet() {
        listOf("F1", "L1", "L2", "L3").forEach {
            assertThat(DefaultReasonCode.classify(it)).isEqualTo(DefaultReasonClassification.OUT_OF_STOCK)
        }
    }

    @Test
    @DisplayName("분류-취소 — 코드 S2 → CANCELLED + 사유 'S2 [영업] 고객사정에 의한 취소'")
    fun classifyCancelled() {
        assertThat(DefaultReasonCode.classify("S2")).isEqualTo(DefaultReasonClassification.CANCELLED)
        assertThat(DefaultReasonCode.displayReason("S2")).isEqualTo("S2 [영업] 고객사정에 의한 취소")
    }

    @Test
    @DisplayName("정의된 취소 코드 전체(L4/O1/S1/S2/S3) → CANCELLED")
    fun cancelledSet() {
        listOf("L4", "O1", "S1", "S2", "S3").forEach {
            assertThat(DefaultReasonCode.classify(it)).isEqualTo(DefaultReasonClassification.CANCELLED)
        }
    }

    @Test
    @DisplayName("미정의 코드 — 'Z9' → CANCELLED, 사유는 코드 원문만")
    fun undefinedCodeIsCancelled() {
        assertThat(DefaultReasonCode.classify("Z9")).isEqualTo(DefaultReasonClassification.CANCELLED)
        assertThat(DefaultReasonCode.displayReason("Z9")).isEqualTo("Z9")
    }

    @Test
    @DisplayName("공백/ null → 분류 대상 아님(null), 사유 null")
    fun blankIsNull() {
        assertThat(DefaultReasonCode.classify(null)).isNull()
        assertThat(DefaultReasonCode.classify("")).isNull()
        assertThat(DefaultReasonCode.classify("   ")).isNull()
        assertThat(DefaultReasonCode.displayReason(null)).isNull()
        assertThat(DefaultReasonCode.displayReason("")).isNull()
        assertThat(DefaultReasonCode.displayReason("  ")).isNull()
    }

    @Test
    @DisplayName("trim 후 판정 — 공백 낀 코드도 정의 코드로 분류/표시")
    fun trimBeforeClassify() {
        assertThat(DefaultReasonCode.classify("  L1  ")).isEqualTo(DefaultReasonClassification.OUT_OF_STOCK)
        assertThat(DefaultReasonCode.displayReason("  L1  ")).isEqualTo("L1 [물류] 재고부족")
        assertThat(DefaultReasonCode.classify(" Z9 ")).isEqualTo(DefaultReasonClassification.CANCELLED)
        assertThat(DefaultReasonCode.displayReason(" Z9 ")).isEqualTo("Z9")
    }
}
