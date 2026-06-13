package com.otoki.powersales.external.sf.auth.audit

import org.springframework.data.jpa.repository.JpaRepository

interface SfInboundAuditRepository : JpaRepository<SfInboundAudit, Long>
