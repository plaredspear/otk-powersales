package com.otoki.powersales.domain.activity.schedule.policy

import com.otoki.powersales.domain.activity.schedule.policy.AbcExemptCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("AbcExemptCode 테스트 (Spec #586)")
class AbcExemptCodeTest {

    @Nested
    @DisplayName("isExempt - 면제 코드 매칭")
    inner class IsExemptTests {

        @ParameterizedTest(name = "[R1] 면제 코드 {0} → true")
        @ValueSource(strings = ["1110", "1120", "1130", "1140", "1210", "1220", "1510", "1530", "1810", "1900"])
        @DisplayName("R1 — 면제 코드 10종 모두 isExempt=true")
        fun isExempt_allLegacyCodes(code: String) {
            assertThat(AbcExemptCode.isExempt(code)).isTrue()
        }

        @Test
        @DisplayName("H1 — '1110' 정확 일치 → true")
        fun isExempt_h1() {
            assertThat(AbcExemptCode.isExempt("1110")).isTrue()
        }

        @Test
        @DisplayName("H2 — '1900' 정확 일치 → true")
        fun isExempt_h2() {
            assertThat(AbcExemptCode.isExempt("1900")).isTrue()
        }

        @Test
        @DisplayName("H3 — '9999' 면제 아닌 코드 → false")
        fun isExempt_h3() {
            assertThat(AbcExemptCode.isExempt("9999")).isFalse()
        }

        @Test
        @DisplayName("E1 — null 입력 → false")
        fun isExempt_e1_null() {
            assertThat(AbcExemptCode.isExempt(null)).isFalse()
        }

        @Test
        @DisplayName("E2 — 빈 문자열 입력 → false")
        fun isExempt_e2_empty() {
            assertThat(AbcExemptCode.isExempt("")).isFalse()
        }

        @Test
        @DisplayName("E3 — 레거시 indexOf 결함 미재현, 부분일치 콤마 입력 '1110,1120' → false")
        fun isExempt_e3_commaDelimited() {
            assertThat(AbcExemptCode.isExempt("1110,1120")).isFalse()
        }

        @Test
        @DisplayName("E4 — 접두사 일치 '11' → false (정확 일치만 true)")
        fun isExempt_e4_prefix() {
            assertThat(AbcExemptCode.isExempt("11")).isFalse()
        }

        @Test
        @DisplayName("E5 — 공백 포함 ' 1110' → false (정규화 미적용)")
        fun isExempt_e5_leadingWhitespace() {
            assertThat(AbcExemptCode.isExempt(" 1110")).isFalse()
            assertThat(AbcExemptCode.isExempt("1110 ")).isFalse()
        }
    }

    @Nested
    @DisplayName("R2 — enum.values vs 레거시 set 정합성")
    inner class LegacyParityTests {

        @Test
        @DisplayName("enum 항목 코드 집합 = 레거시 하드코딩 set")
        fun enumCodes_match_legacySet() {
            val legacy = setOf("1110", "1120", "1130", "1140", "1210", "1220", "1510", "1530", "1810", "1900")
            val enumCodes = AbcExemptCode.entries.map { it.code }.toSet()
            assertThat(enumCodes).isEqualTo(legacy)
        }
    }
}
