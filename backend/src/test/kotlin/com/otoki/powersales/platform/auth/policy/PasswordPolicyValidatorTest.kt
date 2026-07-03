package com.otoki.powersales.platform.auth.policy

import com.otoki.powersales.platform.auth.exception.NewPasswordPolicyViolationException
import com.otoki.powersales.platform.auth.policy.PasswordPolicyValidator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PasswordPolicyValidator 테스트 (8자 이상 + 3종 이상 조합)")
class PasswordPolicyValidatorTest {

    private val validator = PasswordPolicyValidator()

    @Nested
    @DisplayName("validate - 통과 케이스")
    inner class PassCases {

        @Test
        @DisplayName("8자 + 소문자/숫자/특수 3종 -> 통과")
        fun pass_lowerDigitSpecial() {
            assertThatCode { validator.validate("abcd123!") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("임시 비밀번호 pwrs1234! (9자, 소문자/숫자/특수 3종) -> 통과")
        fun pass_temporaryPassword() {
            assertThatCode { validator.validate("pwrs1234!") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("대문자/소문자/숫자 3종 (특수 없음) -> 통과")
        fun pass_upperLowerDigit() {
            assertThatCode { validator.validate("Abcdefg1") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("4종 모두 조합 -> 통과")
        fun pass_allFourTypes() {
            assertThatCode { validator.validate("Abcd123!") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("동일 문자 다수 반복이어도 (반복금지 제거됨) 길이/종류 충족 시 통과")
        fun pass_repeatedAllowed() {
            // 반복금지 규칙 제거 — aaaa 포함이어도 8자 + 3종이면 통과
            assertThatCode { validator.validate("aaaaA1!x") }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("validate - 길이 위반")
    inner class LengthViolations {

        @Test
        @DisplayName("7자 (3종 충족) -> LENGTH_TOO_SHORT 단독")
        fun lengthTooShort() {
            assertThatThrownBy { validator.validate("Abc12!x") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("LENGTH_TOO_SHORT")
                })
        }

        @Test
        @DisplayName("최대 길이 상한 없음 - 64자 (3종) -> 통과")
        fun noMaxLength() {
            val pwd = "Ab1!" + "c".repeat(60)
            assertThat(pwd.length).isEqualTo(64)
            assertThatCode { validator.validate(pwd) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("7자 + 종류 부족 -> LENGTH_TOO_SHORT + INSUFFICIENT_CHARACTER_TYPES")
        fun shortAndInsufficient() {
            assertThatThrownBy { validator.validate("abcdefg") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations)
                        .containsExactlyInAnyOrder("LENGTH_TOO_SHORT", "INSUFFICIENT_CHARACTER_TYPES")
                })
        }
    }

    @Nested
    @DisplayName("validate - 문자 종류 조합 위반")
    inner class CharacterTypeViolations {

        @Test
        @DisplayName("소문자만 8자 (1종) -> INSUFFICIENT_CHARACTER_TYPES")
        fun onlyLower() {
            assertThatThrownBy { validator.validate("abcdefgh") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("INSUFFICIENT_CHARACTER_TYPES")
                })
        }

        @Test
        @DisplayName("소문자+숫자 8자 (2종) -> INSUFFICIENT_CHARACTER_TYPES")
        fun lowerDigitOnly() {
            assertThatThrownBy { validator.validate("abcd1234") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("INSUFFICIENT_CHARACTER_TYPES")
                })
        }

        @Test
        @DisplayName("한글은 어느 카테고리도 아님 - 한글6+숫자2 (8자, 숫자 1종만) -> INSUFFICIENT_CHARACTER_TYPES")
        fun koreanNotCounted() {
            assertThatThrownBy { validator.validate("가나다라마바12") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("INSUFFICIENT_CHARACTER_TYPES")
                })
        }

        @Test
        @DisplayName("대문자+소문자 8자 (2종) -> INSUFFICIENT_CHARACTER_TYPES")
        fun upperLowerOnly() {
            assertThatThrownBy { validator.validate("AbcdEfgh") }
                .isInstanceOf(NewPasswordPolicyViolationException::class.java)
                .satisfies({
                    val ex = it as NewPasswordPolicyViolationException
                    assertThat(ex.violations).containsExactly("INSUFFICIENT_CHARACTER_TYPES")
                })
        }
    }
}
