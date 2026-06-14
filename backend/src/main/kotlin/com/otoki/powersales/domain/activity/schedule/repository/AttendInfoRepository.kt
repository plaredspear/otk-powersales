package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import org.springframework.data.jpa.repository.JpaRepository

interface AttendInfoRepository : JpaRepository<AttendInfo, Long>, AttendInfoRepositoryCustom
