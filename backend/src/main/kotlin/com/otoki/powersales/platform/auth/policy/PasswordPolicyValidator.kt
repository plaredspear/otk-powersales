package com.otoki.powersales.platform.auth.policy

import com.otoki.powersales.platform.auth.exception.NewPasswordPolicyViolationException
import org.springframework.stereotype.Component

/**
 * 비밀번호 정책 검증 (Spec #584 P1-B §2).
 *
 * 정책:
 * - LENGTH_TOO_SHORT: 4자 미만
 * - LENGTH_TOO_LONG: 32자 초과
 * - REPEATED_CHARACTERS: 동일 문자 4회 연속 (한글/특수문자 포함 모든 문자)
 *
 * 위반 규칙 1건 이상 발생 시 [NewPasswordPolicyViolationException] (`details.violations` 에 위반 규칙 코드 배열).
 */
@Component
class PasswordPolicyValidator {

    fun validate(newPassword: String) {
        val violations = collectViolations(newPassword)

        if (violations.isNotEmpty()) {
            throw NewPasswordPolicyViolationException(violations.map { it.name })
        }
    }

    private fun collectViolations(newPassword: String): List<PasswordPolicyViolation> {
        val violations = mutableListOf<PasswordPolicyViolation>()

        when {
            newPassword.length < MIN_LENGTH -> violations += PasswordPolicyViolation.LENGTH_TOO_SHORT
            newPassword.length > MAX_LENGTH -> violations += PasswordPolicyViolation.LENGTH_TOO_LONG
        }

        if (REPEATED_PATTERN.containsMatchIn(newPassword)) {
            violations += PasswordPolicyViolation.REPEATED_CHARACTERS
        }

        return violations
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 32
        private val REPEATED_PATTERN = Regex("(.)\\1\\1\\1")
    }
}

enum class PasswordPolicyViolation {
    LENGTH_TOO_SHORT,
    LENGTH_TOO_LONG,
    REPEATED_CHARACTERS
}
