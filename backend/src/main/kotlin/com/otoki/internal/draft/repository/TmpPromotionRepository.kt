package com.otoki.internal.draft.repository

import com.otoki.internal.draft.entity.TmpPromotion
import org.springframework.data.jpa.repository.JpaRepository

interface TmpPromotionRepository : JpaRepository<TmpPromotion, Long>
