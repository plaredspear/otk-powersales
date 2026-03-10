package com.otoki.internal.admin.dto

sealed interface EffectiveBranchResult {
    data object All : EffectiveBranchResult
    data class Filtered(val codes: List<String>) : EffectiveBranchResult
    data object NoAccess : EffectiveBranchResult
}
