package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionDraft
import org.springframework.data.jpa.repository.JpaRepository

interface SuggestionDraftRepository : JpaRepository<SuggestionDraft, Long> {

    fun findByEmployeeId(employeeId: Long): SuggestionDraft?
}
