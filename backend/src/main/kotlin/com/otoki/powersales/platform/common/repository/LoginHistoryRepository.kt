package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.LoginHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LoginHistoryRepository : JpaRepository<LoginHistory, Long>
