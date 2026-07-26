package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PromotionDetailResponse 테스트")
class PromotionDetailResponseTest {

    @Nested
    @DisplayName("primaryProductStatus - 대표제품 상태 표기")
    inner class PrimaryProductStatus {

        @Test
        @DisplayName("출고중지 제품 → 화면 표시명 '단종'")
        fun outOfStockMapsToDiscontinued() {
            val response = build(primaryProductId = 200L, status = ProductStatus.OUT_OF_STOCK)

            assertThat(response.primaryProductStatus).isEqualTo("단종")
        }

        @Test
        @DisplayName("상태값이 없는 제품 → 화면 표시명 '판매중'")
        fun nullStatusMapsToOnSale() {
            val response = build(primaryProductId = 200L, status = null)

            assertThat(response.primaryProductStatus).isEqualTo("판매중")
        }

        @Test
        @DisplayName("대표제품 자체가 없으면 상태도 null — '판매중' 으로 오인되지 않는다")
        fun noPrimaryProductYieldsNull() {
            val response = build(primaryProductId = null, status = null)

            assertThat(response.primaryProductStatus).isNull()
        }
    }

    private fun build(primaryProductId: Long?, status: ProductStatus?): PromotionDetailResponse =
        PromotionDetailResponse.from(
            promotion = Promotion(
                id = 1L,
                promotionNumber = "PM00000001",
                account = Account(id = 100L, name = "GS25 역삼점"),
                startDate = LocalDate.of(2026, 3, 10),
                endDate = LocalDate.of(2026, 3, 20),
                primaryProductId = primaryProductId
            ),
            accountName = "GS25 역삼점",
            accountCode = "1000",
            primaryProductName = if (primaryProductId != null) "진라면 매운맛 120g" else null,
            primaryProductCode = if (primaryProductId != null) "18010009" else null,
            primaryProductStatus = status,
            targetAmount = 0L,
            actualAmount = 0L
        )
}
