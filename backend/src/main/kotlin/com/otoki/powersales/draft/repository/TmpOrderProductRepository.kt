package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface TmpOrderProductRepository : JpaRepository<TmpOrderProduct, Long>
