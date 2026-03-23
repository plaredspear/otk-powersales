package com.otoki.internal.repository

import com.otoki.internal.entity.LoginHistory
import com.otoki.internal.entity.LoginHistoryId
import org.springframework.data.jpa.repository.JpaRepository

interface LoginHistoryRepository : JpaRepository<LoginHistory, LoginHistoryId>
