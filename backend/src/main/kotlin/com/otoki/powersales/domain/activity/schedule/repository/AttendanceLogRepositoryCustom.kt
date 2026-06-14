package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendanceLogSearchRequest
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AttendanceLogRepositoryCustom {

    fun searchByFilter(filter: AdminAttendanceLogSearchRequest, pageable: Pageable): Page<AttendanceLog>
}
