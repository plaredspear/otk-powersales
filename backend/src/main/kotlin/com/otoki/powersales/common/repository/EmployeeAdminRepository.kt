package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.EmployeeAdmin
import org.springframework.data.jpa.repository.JpaRepository

interface EmployeeAdminRepository : JpaRepository<EmployeeAdmin, String>
