package com.otoki.powersales.external.rdp.auth.audit

import org.springframework.data.jpa.repository.JpaRepository

interface RdpInboundAuditRepository : JpaRepository<RdpInboundAudit, Long>
