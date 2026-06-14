package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.EmployeeAdmin
import org.springframework.data.jpa.repository.JpaRepository

interface EmployeeAdminRepository : JpaRepository<EmployeeAdmin, String>
