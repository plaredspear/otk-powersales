package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.LoginHistory
import org.springframework.data.jpa.repository.JpaRepository

interface LoginHistoryRepository : JpaRepository<LoginHistory, Long>
