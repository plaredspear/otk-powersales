package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.EmployeeAdmin
import org.springframework.data.jpa.repository.JpaRepository

interface EmployeeAdminRepository : JpaRepository<EmployeeAdmin, String>
