package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.SObjectSetting
import org.springframework.data.jpa.repository.JpaRepository

interface SObjectSettingRepository : JpaRepository<SObjectSetting, Long> {
    fun findBySObjectName(sObjectName: String): SObjectSetting?
}
