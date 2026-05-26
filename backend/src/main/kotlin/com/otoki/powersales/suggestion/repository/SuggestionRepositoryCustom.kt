package com.otoki.powersales.suggestion.repository

import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.suggestion.entity.Suggestion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 제안 Repository Custom (Spec #830 P1-B §2.4).
 *
 * admin 권한 검색 — 7종 필터 + 페이징 + soft-delete 제외 + created_at DESC.
 */
interface SuggestionRepositoryCustom {

    fun searchForAdmin(filter: AdminSuggestionFilter, pageable: Pageable): Page<Suggestion>
}
