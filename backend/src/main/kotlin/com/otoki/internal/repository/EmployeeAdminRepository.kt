package com.otoki.internal.repository

import com.otoki.internal.entity.EmployeeAdmin
import org.springframework.data.jpa.repository.JpaRepository

interface EmployeeAdminRepository : JpaRepository<EmployeeAdmin, String>
