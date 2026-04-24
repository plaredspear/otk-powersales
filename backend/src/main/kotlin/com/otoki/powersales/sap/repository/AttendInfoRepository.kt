package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.AttendInfo
import org.springframework.data.jpa.repository.JpaRepository

interface AttendInfoRepository : JpaRepository<AttendInfo, Long>
