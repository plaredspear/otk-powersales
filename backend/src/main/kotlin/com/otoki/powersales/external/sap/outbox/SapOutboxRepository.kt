package com.otoki.powersales.external.sap.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface SapOutboxRepository :
    JpaRepository<SapOutbox, Long>,
    SapOutboxRepositoryCustom
