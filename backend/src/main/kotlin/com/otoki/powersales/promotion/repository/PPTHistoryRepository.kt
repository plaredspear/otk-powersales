package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PPTHistoryRepository : JpaRepository<ProfessionalPromotionTeamHistory, Long> {

    fun findByEmployeeIdOrderByChangedAtDesc(employeeId: Long, pageable: Pageable): Page<ProfessionalPromotionTeamHistory>
}
