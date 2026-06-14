package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpPromotion
import org.springframework.data.jpa.repository.JpaRepository

interface TmpPromotionRepository : JpaRepository<TmpPromotion, Long>
