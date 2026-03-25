package com.otoki.internal.draft.repository

import com.otoki.internal.draft.entity.TmpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface TmpOrderRepository : JpaRepository<TmpOrder, Long>
