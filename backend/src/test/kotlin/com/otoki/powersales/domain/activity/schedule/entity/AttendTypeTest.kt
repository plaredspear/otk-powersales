package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.domain.activity.schedule.enums.AttendType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AttendType Enum 테스트")
class AttendTypeTest {

    @Nested
    @DisplayName("fromCode - 코드로 Enum 조회")
    inner class FromCodeTests {

        @Test
        @DisplayName("유효한 코드 '14' -> ANNUAL_LEAVE 반환")
        fun fromCode_validCode14_returnsAnnualLeave() {
            // Given
            val code = "14"

            // When
            val result = AttendType.fromCode(code)

            // Then
            assertThat(result).isEqualTo(AttendType.ANNUAL_LEAVE)
        }

        @Test
        @DisplayName("모든 코드에 대해 올바른 Enum 반환")
        fun fromCode_allCodes_returnCorrectEnum() {
            // Given / When / Then
            AttendType.entries.forEach { expected ->
                val result = AttendType.fromCode(expected.code)
                assertThat(result)
                    .describedAs("코드 '${expected.code}'에 대해 ${expected.name} 반환")
                    .isEqualTo(expected)
            }
        }

        @Test
        @DisplayName("존재하지 않는 코드 '30' -> null 반환")
        fun fromCode_unknownCode_returnsNull() {
            // Given
            val code = "30"

            // When
            val result = AttendType.fromCode(code)

            // Then
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("빈 문자열 -> null 반환")
        fun fromCode_emptyString_returnsNull() {
            // Given
            val code = ""

            // When
            val result = AttendType.fromCode(code)

            // Then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("ANNUAL_LEAVE_CODES - 연차 코드 집합")
    inner class AnnualLeaveCodesTests {

        @Test
        @DisplayName("연차 코드가 정확히 6개 코드를 포함")
        fun annualLeaveCodes_containsExactCodes() {
            // Given
            val expectedCodes = setOf("10", "14", "20", "90", "120", "133")

            // When
            val result = AttendType.ANNUAL_LEAVE_CODES

            // Then
            assertThat(result).containsExactlyInAnyOrderElementsOf(expectedCodes)
        }
    }

    @Nested
    @DisplayName("isAnnualLeave - 연차 여부")
    inner class IsAnnualLeaveTests {

        @Test
        @DisplayName("모든 항목의 isAnnualLeave가 true")
        fun allEntries_isAnnualLeaveTrue() {
            // Given / When / Then
            AttendType.entries.forEach { attendType ->
                assertThat(attendType.isAnnualLeave)
                    .describedAs("${attendType.name}(${attendType.code})의 isAnnualLeave")
                    .isTrue()
            }
        }
    }
}
