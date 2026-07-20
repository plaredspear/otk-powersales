package com.otoki.powersales.external.ovip.auth.audit

import org.springframework.data.jpa.repository.JpaRepository

interface OvipInboundAuditRepository : JpaRepository<OvipInboundAudit, Long>
