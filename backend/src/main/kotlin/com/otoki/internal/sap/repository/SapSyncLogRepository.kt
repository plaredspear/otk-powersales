package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.SapSyncLog
import org.springframework.data.jpa.repository.JpaRepository

interface SapSyncLogRepository : JpaRepository<SapSyncLog, Long>
