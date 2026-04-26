package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.LoginHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LoginHistoryRepository : JpaRepository<LoginHistory, Long>
