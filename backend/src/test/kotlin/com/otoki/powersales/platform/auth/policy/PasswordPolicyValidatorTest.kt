package com.otoki.powersales.platform.auth.policy

import com.otoki.powersales.platform.auth.exception.NewPasswordPolicyViolationException
import com.otoki.powersales.platform.auth.policy.PasswordPolicyValidator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PasswordPolicyValidator 테스트")
class PasswordPolicyValidatorTest {

    private val validator = PasswordPolicyValidator()

    @Nested
    @DisplayName("validate - 통과 케이스")
    inner class PassCases {

        @Test
        @DisplayName("최소 길이 - 4자 영문 -> 통과")
        fun pass_minLengthAlpha() {
            assertThatCode { validator.validate("abcd") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("한글/특수문자 혼합 - 정책 위반 없음 -> 통과")
        fun pass_mixedKoreanSpecial() {
            assertThatCode { validator.validate("abcd1234!@한") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("\"1234\" 입력 - 임시 비밀번호 동일 차단 제거됨 -> 통과")
        fun pass_temporaryPasswordAllowed() {
            assertThatCode { validator.validate("1234") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("최대 길이 - 32자, 반복 없음 -> 통과")
        fun pass_maxLength() {
            // 동일 문자 4회 연속 회피: abc 패턴 반복 (32자)
            val pwd = (1..32).joinToString("") { ('a' + (it % 26)).toString() }
            assertThat(pwd.length).isEqualTo(32)
            assertThatCode { validator.validate(pwd) }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("validate - 길이 위반")
    inner class LengthViolations {

        @Test
        @DisplayName("3자 입력 -> LENGTH_TOO_SHORT")
        fun lengthTooShort() {
            assertThatThrownBy { validator.validate("abc") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("LENGTH_TOO_SHORT")
                })
        }

        @Test
        @DisplayName("33자 입력 -> LENGTH_TOO_LONG")
        fun lengthTooLong() {
            val pwd = "a".repeat(33)
            // "a" 33회 반복 → REPEATED_CHARACTERS 도 함께 발생 (수집됨)
            assertThatThrownBy { validator.validate(pwd) }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).contains("LENGTH_TOO_LONG", "REPEATED_CHARACTERS")
                })
        }

        @Test
        @DisplayName("33자 단조 증가 (반복 없음) -> LENGTH_TOO_LONG 단독")
        fun lengthTooLongOnly() {
            val pwd = (1..33).joinToString("") { ('a' + (it % 26)).toString() }
            assertThatThrownBy { validator.validate(pwd) }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("LENGTH_TOO_LONG")
                })
        }
    }

    @Nested
    @DisplayName("validate - 반복 문자 위반")
    inner class RepeatedViolations {

        @Test
        @DisplayName("영문 4연속 -> REPEATED_CHARACTERS")
        fun repeatedAlpha() {
            assertThatThrownBy { validator.validate("aaaa") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("REPEATED_CHARACTERS")
                })
        }

        @Test
        @DisplayName("한글 4연속 -> REPEATED_CHARACTERS")
        fun repeatedKorean() {
            assertThatThrownBy { validator.validate("가가가가") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("REPEATED_CHARACTERS")
                })
        }

        @Test
        @DisplayName("특수문자 4연속 -> REPEATED_CHARACTERS")
        fun repeatedSpecial() {
            assertThatThrownBy { validator.validate("!!!!") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("REPEATED_CHARACTERS")
                })
        }

        @Test
        @DisplayName("부분 반복 (abcaaaa) -> REPEATED_CHARACTERS")
        fun partialRepeated() {
            assertThatThrownBy { validator.validate("abcaaaa") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("REPEATED_CHARACTERS")
                })
        }
    }

    @Nested
    @DisplayName("validate - 위반 우선순위")
    inner class PriorityRules {

        @Test
        @DisplayName("\"123\" 입력 -> 길이 위반")
        fun shortPassword() {
            assertThatThrownBy { validator.validate("123") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("LENGTH_TOO_SHORT")
                })
        }
    }
}
