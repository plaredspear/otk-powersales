package com.otoki.powersales.external.common.outboundlog.repository

import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalApiLogRepository : JpaRepository<ExternalApiLog, Long>
