package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.SharingRuleTarget
import org.springframework.data.jpa.repository.JpaRepository

interface SharingRuleTargetRepository : JpaRepository<SharingRuleTarget, Long> {
    fun findAllBySharingRuleId(sharingRuleId: Long): List<SharingRuleTarget>
    fun deleteAllBySharingRuleId(sharingRuleId: Long)
}
