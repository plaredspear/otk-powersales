package com.otoki.internal.draft.repository

import com.otoki.internal.draft.entity.TmpSuggest
import org.springframework.data.jpa.repository.JpaRepository

interface TmpSuggestRepository : JpaRepository<TmpSuggest, Long>
