package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.SharingRuleCondition
import org.springframework.data.jpa.repository.JpaRepository

interface SharingRuleConditionRepository : JpaRepository<SharingRuleCondition, Long> {
    fun findAllBySharingRuleIdOrderByConditionOrderAsc(sharingRuleId: Long): List<SharingRuleCondition>
    fun deleteAllBySharingRuleId(sharingRuleId: Long)
}
