package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PPTHistoryRepository : JpaRepository<ProfessionalPromotionTeamHistory, Long>, PPTHistoryRepositoryCustom
