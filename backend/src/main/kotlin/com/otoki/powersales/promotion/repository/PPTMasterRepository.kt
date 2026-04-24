package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import org.springframework.data.jpa.repository.JpaRepository

interface PPTMasterRepository : JpaRepository<ProfessionalPromotionTeamMaster, Long>, PPTMasterRepositoryCustom {

    fun findByEmployeeIdAndEndDateIsNull(employeeId: Long): List<ProfessionalPromotionTeamMaster>

    fun findByEmployeeId(employeeId: Long): List<ProfessionalPromotionTeamMaster>
}
