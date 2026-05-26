package com.otoki.powersales.suggestion.dto.admin

import com.otoki.powersales.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.suggestion.entity.SuggestionCategory
import java.time.LocalDateTime

/**
 * admin 제안 목록 검색 조건 wrapper (Spec #830 P1-B §2.4).
 *
 * 7종 필터 + 페이징. null/blank 필드는 검색 조건에서 무시.
 */
data class AdminSuggestionFilter(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val category: SuggestionCategory? = null,
    val employeeName: String? = null,
    val accountCode: String? = null,
    val actionStatus: SuggestionActionStatus? = null,
    val productCode: String? = null
)
