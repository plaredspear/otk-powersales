package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoSearchRequest
import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AttendInfoRepositoryCustom {

    fun searchByFilter(filter: AdminAttendInfoSearchRequest, pageable: Pageable): Page<AttendInfo>
}
