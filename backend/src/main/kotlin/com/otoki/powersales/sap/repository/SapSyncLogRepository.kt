package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.SapSyncLog
import org.springframework.data.jpa.repository.JpaRepository

interface SapSyncLogRepository : JpaRepository<SapSyncLog, Long>
