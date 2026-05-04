package com.otoki.powersales.schedule.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AccountCoordinateParser 테스트 (Spec #585 §6)")
class AccountCoordinateParserTest {

    @Nested
    @DisplayName("parse - 정상 변환")
    inner class ValidParseTests {

        @Test
        @DisplayName("정상 위경도 String → Double 변환")
        fun valid_normalCoordinates() {
            val result = AccountCoordinateParser.parse("37.4979", "127.0276")

            assertThat(result).isInstanceOf(AccountCoordinateParser.Coords.Valid::class.java)
            val valid = result as AccountCoordinateParser.Coords.Valid
            assertThat(valid.latitude).isEqualTo(37.4979)
            assertThat(valid.longitude).isEqualTo(127.0276)
        }

        @Test
        @DisplayName("앞뒤 공백 trim 후 정상 변환")
        fun valid_trimsWhitespace() {
            val result = AccountCoordinateParser.parse("  37.4979  ", " 127.0276 ")

            assertThat(result).isInstanceOf(AccountCoordinateParser.Coords.Valid::class.java)
        }

        @Test
        @DisplayName("경계값 ±90 / ±180 정상 변환")
        fun valid_atBoundary() {
            val north = AccountCoordinateParser.parse("90.0", "180.0")
            val south = AccountCoordinateParser.parse("-90.0", "-180.0")

            assertThat(north).isInstanceOf(AccountCoordinateParser.Coords.Valid::class.java)
            assertThat(south).isInstanceOf(AccountCoordinateParser.Coords.Valid::class.java)
        }
    }

    @Nested
    @DisplayName("parse - Missing 케이스 (5종)")
    inner class MissingCasesTests {

        @Test
        @DisplayName("①  null → Missing")
        fun missing_null() {
            assertThat(AccountCoordinateParser.parse(null, "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
            assertThat(AccountCoordinateParser.parse("37.4979", null))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }

        @Test
        @DisplayName("② 빈 문자열 → Missing")
        fun missing_empty() {
            assertThat(AccountCoordinateParser.parse("", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }

        @Test
        @DisplayName("③ 공백만 포함 → Missing")
        fun missing_blank() {
            assertThat(AccountCoordinateParser.parse("   ", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
            assertThat(AccountCoordinateParser.parse("\t\n", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }

        @Test
        @DisplayName("④ Double 파싱 실패 → Missing")
        fun missing_parseFailure() {
            assertThat(AccountCoordinateParser.parse("abc", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
            assertThat(AccountCoordinateParser.parse("37.4979", "ABC"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }

        @Test
        @DisplayName("⑤-a 위도 +90 초과 → Missing")
        fun missing_latitudeOverMax() {
            assertThat(AccountCoordinateParser.parse("90.1", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
            assertThat(AccountCoordinateParser.parse("91.0", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }

        @Test
        @DisplayName("⑤-b 위도 -90 미만 → Missing")
        fun missing_latitudeUnderMin() {
            assertThat(AccountCoordinateParser.parse("-90.1", "127.0276"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }

        @Test
        @DisplayName("⑤-c 경도 ±180 초과 → Missing")
        fun missing_longitudeOutOfRange() {
            assertThat(AccountCoordinateParser.parse("37.4979", "180.1"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
            assertThat(AccountCoordinateParser.parse("37.4979", "-180.1"))
                .isEqualTo(AccountCoordinateParser.Coords.Missing)
        }
    }
}
