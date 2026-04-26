package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpPromotion
import org.springframework.data.jpa.repository.JpaRepository

interface TmpPromotionRepository : JpaRepository<TmpPromotion, Long>
