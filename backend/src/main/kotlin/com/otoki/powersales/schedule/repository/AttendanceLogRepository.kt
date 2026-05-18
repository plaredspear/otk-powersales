package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.AttendanceLog
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceLogRepository : JpaRepository<AttendanceLog, Long>, AttendanceLogRepositoryCustom
