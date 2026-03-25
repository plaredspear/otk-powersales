package com.otoki.internal.draft.repository

import com.otoki.internal.draft.entity.TmpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface TmpOrderProductRepository : JpaRepository<TmpOrderProduct, Long>
