package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceLogRepository : JpaRepository<AttendanceLog, Long>, AttendanceLogRepositoryCustom
