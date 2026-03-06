package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.AttendInfo
import org.springframework.data.jpa.repository.JpaRepository

interface AttendInfoRepository : JpaRepository<AttendInfo, Long>
