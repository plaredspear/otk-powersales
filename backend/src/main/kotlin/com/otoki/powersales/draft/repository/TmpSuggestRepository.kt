package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpSuggest
import org.springframework.data.jpa.repository.JpaRepository

interface TmpSuggestRepository : JpaRepository<TmpSuggest, Long>
