package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.SharingRecalcLog
import org.springframework.data.jpa.repository.JpaRepository

interface SharingRecalcLogRepository : JpaRepository<SharingRecalcLog, Long> {
    fun findTopByOrderByTriggeredAtDesc(): SharingRecalcLog?
}
