package com.otoki.powersales.sap.outbound.repository

import com.otoki.powersales.sap.outbound.entity.SapOutboundLog
import org.springframework.data.jpa.repository.JpaRepository

interface SapOutboundLogRepository : JpaRepository<SapOutboundLog, Long>
