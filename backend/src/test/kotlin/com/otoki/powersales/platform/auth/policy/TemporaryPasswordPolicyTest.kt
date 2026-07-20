package com.otoki.powersales.platform.auth.policy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TemporaryPasswordPolicy - 임시 비밀번호 발급 규칙")
class TemporaryPasswordPolicyTest {

    @Nested
    @DisplayName("forEmployeeCode")
    inner class ForEmployeeCode {

        @Test
        @DisplayName("사번이 있으면 {사번}@pwrs 를 반환한다")
        fun withEmployeeCode() {
            assertThat(TemporaryPasswordPolicy.forEmployeeCode("100123")).isEqualTo("100123@pwrs")
        }

        @Test
        @DisplayName("사번이 null 이면 종전 고정값으로 되돌아간다")
        fun nullEmployeeCode() {
            assertThat(TemporaryPasswordPolicy.forEmployeeCode(null))
                .isEqualTo(TemporaryPasswordPolicy.FALLBACK_PASSWORD)
        }

        @Test
        @DisplayName("사번이 공백만이면 종전 고정값으로 되돌아간다")
        fun blankEmployeeCode() {
            assertThat(TemporaryPasswordPolicy.forEmployeeCode("   "))
                .isEqualTo(TemporaryPasswordPolicy.FALLBACK_PASSWORD)
        }

        @Test
        @DisplayName("발급된 평문은 비밀번호 정책(8자 이상 + 3종 이상 조합) 을 충족한다")
        fun satisfiesPasswordPolicy() {
            val validator = PasswordPolicyValidator()

            // 운영 사번은 항상 8자 이상이라는 전제 (사용자 확인) 아래, 조립 결과가 정책을 통과하는지 확인.
            val password = TemporaryPasswordPolicy.forEmployeeCode("20100123")

            assertThat(password).isEqualTo("20100123@pwrs")
            assertThat(password.length).isGreaterThanOrEqualTo(PasswordPolicyValidator.MIN_LENGTH)
            validator.validate(password)
        }
    }
}
