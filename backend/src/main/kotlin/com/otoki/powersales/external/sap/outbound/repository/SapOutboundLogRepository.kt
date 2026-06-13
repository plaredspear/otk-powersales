package com.otoki.powersales.external.sap.outbound.repository

import com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog
import org.springframework.data.jpa.repository.JpaRepository

interface SapOutboundLogRepository :
    JpaRepository<SapOutboundLog, Long>,
    SapOutboundLogRepositoryCustom
