package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpSuggest
import org.springframework.data.jpa.repository.JpaRepository

interface TmpSuggestRepository : JpaRepository<TmpSuggest, Long>
