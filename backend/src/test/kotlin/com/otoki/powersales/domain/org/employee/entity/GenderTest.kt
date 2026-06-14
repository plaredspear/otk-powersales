package com.otoki.powersales.domain.org.employee.entity

import com.otoki.powersales.domain.org.employee.enums.Gender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Gender enum 테스트")
class GenderTest {

    @Nested
    @DisplayName("fromSapCode - SAP 코드 매핑")
    inner class FromSapCode {

        @Test
        @DisplayName("코드 '1' -> MALE")
        fun fromSapCode_male() {
            assertThat(Gender.fromSapCode("1")).isEqualTo(Gender.MALE)
        }

        @Test
        @DisplayName("코드 '2' -> FEMALE")
        fun fromSapCode_female() {
            assertThat(Gender.fromSapCode("2")).isEqualTo(Gender.FEMALE)
        }

        @Test
        @DisplayName("알 수 없는 코드 '9' -> null (예외 없음)")
        fun fromSapCode_unknown() {
            assertThat(Gender.fromSapCode("9")).isNull()
        }

        @Test
        @DisplayName("빈 문자열 -> null")
        fun fromSapCode_empty() {
            assertThat(Gender.fromSapCode("")).isNull()
        }

        @Test
        @DisplayName("null 입력 -> null")
        fun fromSapCode_null() {
            assertThat(Gender.fromSapCode(null)).isNull()
        }

        @Test
        @DisplayName("공백 포함 ' 1 ' -> null (트림 미지원, 엄격 매칭)")
        fun fromSapCode_withWhitespace() {
            assertThat(Gender.fromSapCode(" 1 ")).isNull()
        }
    }

    @Nested
    @DisplayName("enum 프로퍼티")
    inner class Properties {

        @Test
        @DisplayName("MALE.sapCode = '1', displayName = '남', name = 'MALE'")
        fun male_properties() {
            assertThat(Gender.MALE.sapCode).isEqualTo("1")
            assertThat(Gender.MALE.displayName).isEqualTo("남")
            assertThat(Gender.MALE.name).isEqualTo("MALE")
        }

        @Test
        @DisplayName("FEMALE.sapCode = '2', displayName = '여', name = 'FEMALE'")
        fun female_properties() {
            assertThat(Gender.FEMALE.sapCode).isEqualTo("2")
            assertThat(Gender.FEMALE.displayName).isEqualTo("여")
            assertThat(Gender.FEMALE.name).isEqualTo("FEMALE")
        }
    }
}
