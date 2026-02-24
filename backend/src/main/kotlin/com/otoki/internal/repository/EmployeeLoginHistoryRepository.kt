package com.otoki.internal.repository

import com.otoki.internal.entity.EmployeeLoginHistory
import com.otoki.internal.entity.EmployeeLoginHistoryId
import org.springframework.data.jpa.repository.JpaRepository

interface EmployeeLoginHistoryRepository : JpaRepository<EmployeeLoginHistory, EmployeeLoginHistoryId>
