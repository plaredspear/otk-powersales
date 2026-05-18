package com.otoki.powersales.sap.auth.audit

import org.springframework.data.jpa.repository.JpaRepository

interface SapInboundAuditRepository :
    JpaRepository<SapInboundAudit, Long>,
    SapInboundAuditRepositoryCustom
