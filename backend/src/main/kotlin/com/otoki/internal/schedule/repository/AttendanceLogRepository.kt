package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.AttendanceLog
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceLogRepository : JpaRepository<AttendanceLog, Long>
