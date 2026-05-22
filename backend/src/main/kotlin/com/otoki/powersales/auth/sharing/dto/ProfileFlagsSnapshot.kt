package com.otoki.powersales.auth.sharing.dto

/**
 * Profile system 권한 비트 평가 결과 immutable snapshot (spec #782 P2-B).
 *
 * ProfileFlagsEvaluator 의 반환 — sharing policy evaluator 의 권한 매트릭스 최우선 분기에 사용.
 */
data class ProfileFlagsSnapshot(
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val viewAllUsers: Boolean,
    val manageUsers: Boolean,
    val apiEnabled: Boolean,
) {
    companion object {
        val NONE = ProfileFlagsSnapshot(
            viewAllData = false,
            modifyAllData = false,
            viewAllUsers = false,
            manageUsers = false,
            apiEnabled = false,
        )
    }
}
