package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.AgreementHistory
import org.springframework.data.jpa.repository.JpaRepository

interface AgreementHistoryRepository : JpaRepository<AgreementHistory, Long>
