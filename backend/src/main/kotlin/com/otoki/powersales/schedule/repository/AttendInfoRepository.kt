package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.AttendInfo
import org.springframework.data.jpa.repository.JpaRepository

interface AttendInfoRepository : JpaRepository<AttendInfo, Long>
