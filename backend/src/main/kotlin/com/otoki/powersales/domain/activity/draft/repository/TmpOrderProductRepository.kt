package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface TmpOrderProductRepository : JpaRepository<TmpOrderProduct, Long>
