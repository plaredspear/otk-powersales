package com.otoki.powersales.auth.policy

import com.otoki.powersales.auth.exception.NewPasswordPolicyViolationException
import com.otoki.powersales.auth.exception.NewPasswordSameAsTemporaryException
import com.otoki.powersales.employee.service.AdminEmployeeCredentialService
import org.springframework.stereotype.Component

/**
 * 비밀번호 정책 검증 (Spec #584 P1-B §2).
 *
 * 정책:
 * - LENGTH_TOO_SHORT: 4자 미만
 * - LENGTH_TOO_LONG: 32자 초과
 * - REPEATED_CHARACTERS: 동일 문자 4회 연속 (한글/특수문자 포함 모든 문자)
 * - SAME_AS_TEMPORARY: 임시 비밀번호("1234") 와 동일
 *
 * 분기 규칙:
 * - SAME_AS_TEMPORARY 단독 위반 시 [NewPasswordSameAsTemporaryException] 발생
 * - 다른 규칙 1건 이상 위반 시 [NewPasswordPolicyViolationException] (`details.violations` 에 위반 규칙 코드 배열)
 *   - 길이/반복 위반과 SAME_AS_TEMPORARY 가 함께 발생할 경우 길이/반복 위반 코드를 우선시한다 (LENGTH_TOO_SHORT 등이
 *     더 본질적인 오류이므로). 즉 SAME_AS_TEMPORARY 는 다른 위반이 없을 때만 별도 코드로 응답한다.
 */
@Component
class PasswordPolicyValidator {

    fun validate(newPassword: String) {
        val violations = collectViolations(newPassword)
        val structural = violations.filter { it != PasswordPolicyViolation.SAME_AS_TEMPORARY }

        if (structural.isNotEmpty()) {
            throw NewPasswordPolicyViolationException(structural.map { it.name })
        }

        if (PasswordPolicyViolation.SAME_AS_TEMPORARY in violations) {
            throw NewPasswordSameAsTemporaryException()
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

        if (newPassword == AdminEmployeeCredentialService.TEMPORARY_PASSWORD) {
            violations += PasswordPolicyViolation.SAME_AS_TEMPORARY
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
    REPEATED_CHARACTERS,
    SAME_AS_TEMPORARY
}
