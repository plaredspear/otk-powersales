package com.otoki.powersales.platform.auth.policy

import com.otoki.powersales.platform.auth.exception.NewPasswordPolicyViolationException
import org.springframework.stereotype.Component

/**
 * 비밀번호 정책 검증 (로그인 후 비밀번호 변경 — web + mobile 공용).
 *
 * 정책:
 * - LENGTH_TOO_SHORT: 8자 미만
 * - INSUFFICIENT_CHARACTER_TYPES: 영문 대문자 / 영문 소문자 / 숫자 / 특수문자 중 3종 미만 조합
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

        if (newPassword.length < MIN_LENGTH) {
            violations += PasswordPolicyViolation.LENGTH_TOO_SHORT
        }

        if (countCharacterTypes(newPassword) < MIN_CHARACTER_TYPES) {
            violations += PasswordPolicyViolation.INSUFFICIENT_CHARACTER_TYPES
        }

        return violations
    }

    private fun countCharacterTypes(password: String): Int {
        var types = 0
        if (password.any { it in 'A'..'Z' }) types++
        if (password.any { it in 'a'..'z' }) types++
        if (password.any { it.isDigit() }) types++
        if (password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) types++
        return types
    }

    companion object {
        const val MIN_LENGTH = 8
        const val MIN_CHARACTER_TYPES = 3
    }
}

enum class PasswordPolicyViolation {
    LENGTH_TOO_SHORT,
    INSUFFICIENT_CHARACTER_TYPES
}
