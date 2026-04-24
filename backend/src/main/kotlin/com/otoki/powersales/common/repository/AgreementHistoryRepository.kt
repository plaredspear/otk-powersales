package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.AgreementHistory
import org.springframework.data.jpa.repository.JpaRepository

interface AgreementHistoryRepository : JpaRepository<AgreementHistory, Long>
