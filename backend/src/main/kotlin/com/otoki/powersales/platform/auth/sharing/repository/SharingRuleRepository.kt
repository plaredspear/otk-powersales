package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.SharingRule
import org.springframework.data.jpa.repository.JpaRepository

interface SharingRuleRepository : JpaRepository<SharingRule, Long> {
    fun findByDeveloperName(developerName: String): SharingRule?
    fun findAllBySObjectName(sObjectName: String): List<SharingRule>
}
