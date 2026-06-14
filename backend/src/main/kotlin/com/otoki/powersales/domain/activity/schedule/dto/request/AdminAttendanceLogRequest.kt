package com.otoki.powersales.domain.activity.schedule.dto.request

import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import java.time.LocalDate

data class AdminAttendanceLogSearchRequest(
    val employeeId: Long? = null,
    val accountId: Long? = null,
    val attendanceType: AttendanceType? = null,
    val attendanceDateFrom: LocalDate? = null,
    val attendanceDateTo: LocalDate? = null,
    val keyword: String? = null,
)
