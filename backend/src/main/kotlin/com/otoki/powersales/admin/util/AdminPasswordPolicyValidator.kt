package com.otoki.powersales.admin.util

import com.otoki.powersales.admin.exception.AdminPasswordPolicyViolationException

/**
 * 시스템 관리자 수동 등록 전용 비밀번호 정책 검증 유틸 (Spec #579).
 *
 * 일반 사용자 비밀번호 변경(`AuthService.validateNewPassword`, 4자 이상 + 동일 문자 금지)과는
 * 별도의 강화 정책이며, 본 유틸은 관리자 등록 경로에서만 사용된다.
 *
 * 정책:
 * - 길이 8~64자
 * - 영문 대/소문자, 숫자, 특수문자 중 2종 이상 조합
 * - 동일 문자 4회 연속 반복 금지
 */
object AdminPasswordPolicyValidator {

    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 64
    private const val MIN_CATEGORIES = 2
    private const val MAX_CONSECUTIVE_SAME_CHAR = 4

    private val SPECIAL_CHARS = "!@#$%^&*()_+-=[]{};':\"|,.<>/?".toSet()

    fun validate(password: String) {
        if (password.length < MIN_LENGTH || password.length > MAX_LENGTH) {
            throw AdminPasswordPolicyViolationException(
                "비밀번호는 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다"
            )
        }

        if (countCategories(password) < MIN_CATEGORIES) {
            throw AdminPasswordPolicyViolationException(
                "영문, 숫자, 특수문자 중 2종 이상을 조합해주세요"
            )
        }

        if (hasConsecutiveSameChars(password, MAX_CONSECUTIVE_SAME_CHAR)) {
            throw AdminPasswordPolicyViolationException(
                "동일한 문자를 ${MAX_CONSECUTIVE_SAME_CHAR}회 이상 연속 사용할 수 없습니다"
            )
        }
    }

    private fun countCategories(password: String): Int {
        var categories = 0
        if (password.any { it.isUpperCase() }) categories++
        if (password.any { it.isLowerCase() }) categories++
        if (password.any { it.isDigit() }) categories++
        if (password.any { it in SPECIAL_CHARS }) categories++
        return categories
    }

    private fun hasConsecutiveSameChars(password: String, threshold: Int): Boolean {
        if (password.length < threshold) return false
        var run = 1
        for (i in 1 until password.length) {
            if (password[i] == password[i - 1]) {
                run++
                if (run >= threshold) return true
            } else {
                run = 1
            }
        }
        return false
    }
}
