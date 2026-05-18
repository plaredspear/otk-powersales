package com.otoki.powersales.sf.auth.audit

import org.springframework.data.jpa.repository.JpaRepository

interface SfInboundAuditRepository : JpaRepository<SfInboundAudit, Long>
