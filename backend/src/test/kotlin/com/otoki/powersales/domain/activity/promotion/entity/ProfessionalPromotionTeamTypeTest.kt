package com.otoki.powersales.domain.activity.promotion.entity

import com.otoki.powersales.domain.activity.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProfessionalPromotionTeamType enum 테스트")
class ProfessionalPromotionTeamTypeTest {

    @Nested
    @DisplayName("displayName 매핑")
    inner class DisplayNameTests {

        @Test
        @DisplayName("5개 값 모두 한글 displayName 보유 (V139: GENERAL 제거 — null 의미 매핑)")
        fun allValuesHaveDisplayName() {
            assertThat(ProfessionalPromotionTeamType.RAMEN_SALE.displayName).isEqualTo("라면세일조")
            assertThat(ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED.displayName).isEqualTo("프레시세일조_냉장")
            assertThat(ProfessionalPromotionTeamType.FRESH_SALE_FROZEN.displayName).isEqualTo("프레시세일조_냉동")
            assertThat(ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING.displayName).isEqualTo("프레시세일조_만두")
            assertThat(ProfessionalPromotionTeamType.CURRY_PROMOTION.displayName).isEqualTo("카레행사조")
        }
    }

    @Nested
    @DisplayName("fromDisplayName")
    inner class FromDisplayNameTests {

        @Test
        @DisplayName("성공 - 유효한 한글 -> enum 반환")
        fun fromDisplayName_success() {
            assertThat(ProfessionalPromotionTeamType.fromDisplayName("라면세일조"))
                .isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
            assertThat(ProfessionalPromotionTeamType.fromDisplayName("프레시세일조_냉장"))
                .isEqualTo(ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)
        }

        @Test
        @DisplayName("실패 - 매칭되지 않는 값 -> IllegalArgumentException")
        fun fromDisplayName_invalid() {
            assertThatThrownBy { ProfessionalPromotionTeamType.fromDisplayName("존재하지않는값") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("유효하지 않은 전문행사조 유형")
        }
    }

    @Nested
    @DisplayName("fromDisplayNameOrNull")
    inner class FromDisplayNameOrNullTests {

        @Test
        @DisplayName("성공 - 유효한 한글 -> enum 반환")
        fun fromDisplayNameOrNull_success() {
            assertThat(ProfessionalPromotionTeamType.fromDisplayNameOrNull("카레행사조"))
                .isEqualTo(ProfessionalPromotionTeamType.CURRY_PROMOTION)
        }

        @Test
        @DisplayName("null/blank/유효하지 않은 값 -> null 반환")
        fun fromDisplayNameOrNull_returnsNull() {
            assertThat(ProfessionalPromotionTeamType.fromDisplayNameOrNull(null)).isNull()
            assertThat(ProfessionalPromotionTeamType.fromDisplayNameOrNull("")).isNull()
            assertThat(ProfessionalPromotionTeamType.fromDisplayNameOrNull("  ")).isNull()
            assertThat(ProfessionalPromotionTeamType.fromDisplayNameOrNull("이상한값")).isNull()
        }
    }

    @Nested
    @DisplayName("AttributeConverter")
    inner class ConverterTests {

        private val converter = ProfessionalPromotionTeamTypeConverter()

        @Test
        @DisplayName("Entity -> DB: enum의 displayName 반환")
        fun convertToDatabaseColumn_success() {
            assertThat(converter.convertToDatabaseColumn(ProfessionalPromotionTeamType.RAMEN_SALE))
                .isEqualTo("라면세일조")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
        }

        @Test
        @DisplayName("DB -> Entity: 한글 -> enum")
        fun convertToEntityAttribute_success() {
            assertThat(converter.convertToEntityAttribute("카레행사조"))
                .isEqualTo(ProfessionalPromotionTeamType.CURRY_PROMOTION)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("")).isNull()
        }
    }
}
