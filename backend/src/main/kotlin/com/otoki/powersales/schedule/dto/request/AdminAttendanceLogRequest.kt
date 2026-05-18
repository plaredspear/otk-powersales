package com.otoki.powersales.schedule.dto.request

import com.otoki.powersales.schedule.enums.AttendanceType
import java.time.LocalDate

data class AdminAttendanceLogSearchRequest(
    val employeeId: Long? = null,
    val accountId: Int? = null,
    val attendanceType: AttendanceType? = null,
    val attendanceDateFrom: LocalDate? = null,
    val attendanceDateTo: LocalDate? = null,
    val keyword: String? = null,
)
