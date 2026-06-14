package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.AttendType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.external.sap.inbound.dto.attendance.ScheduleConversionSummary
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminAttendInfoListItemResponse(
    val id: Long,
    val name: String?,
    val employeeCode: String,
    val employeeName: String?,
    val employeeJobCode: String?,
    val attendType: String?,
    val attendTypeName: String?,
    val startDate: String,
    val endDate: String?,
    val status: String?,
    val createdAt: LocalDateTime,
    val createdByName: String?,
) {
    companion object {
        fun from(entity: AttendInfo, employee: Employee?): AdminAttendInfoListItemResponse =
            AdminAttendInfoListItemResponse(
                id = entity.id,
                name = entity.name,
                employeeCode = entity.employeeCode,
                employeeName = employee?.name,
                employeeJobCode = employee?.jobCode,
                attendType = entity.attendType,
                attendTypeName = entity.attendType?.let { AttendType.fromCode(it)?.displayName },
                startDate = entity.startDate,
                endDate = entity.endDate,
                status = entity.status,
                createdAt = entity.createdAt,
                createdByName = entity.createdBy?.name,
            )
    }
}

data class LinkedSchedulePreview(
    val id: Long,
    val workingDate: LocalDate,
    val workingType: String,
)

data class AdminAttendInfoDetailResponse(
    val id: Long,
    val name: String?,
    val employeeCode: String,
    val employeeName: String?,
    val employeeJobCode: String?,
    val attendType: String?,
    val attendTypeName: String?,
    val startDate: String,
    val endDate: String?,
    val status: String?,
    val createdAt: LocalDateTime,
    val createdByName: String?,
    val updatedAt: LocalDateTime,
    val lastModifiedByName: String?,
    val linkedScheduleCount: Int,
    val linkedSchedules: List<LinkedSchedulePreview>,
    val conversionSummary: ScheduleConversionSummary?,
) {
    companion object {
        fun from(
            entity: AttendInfo,
            employee: Employee?,
            linkedSchedules: List<TeamMemberSchedule>,
            conversionSummary: ScheduleConversionSummary? = null,
        ): AdminAttendInfoDetailResponse =
            AdminAttendInfoDetailResponse(
                id = entity.id,
                name = entity.name,
                employeeCode = entity.employeeCode,
                employeeName = employee?.name,
                employeeJobCode = employee?.jobCode,
                attendType = entity.attendType,
                attendTypeName = entity.attendType?.let { AttendType.fromCode(it)?.displayName },
                startDate = entity.startDate,
                endDate = entity.endDate,
                status = entity.status,
                createdAt = entity.createdAt,
                createdByName = entity.createdBy?.name,
                updatedAt = entity.updatedAt,
                lastModifiedByName = entity.lastModifiedBy?.name,
                linkedScheduleCount = linkedSchedules.size,
                linkedSchedules = linkedSchedules.take(5).map {
                    LinkedSchedulePreview(
                        id = it.id,
                        workingDate = it.workingDate!!,
                        workingType = it.workingType?.displayName ?: "연차",
                    )
                },
                conversionSummary = conversionSummary,
            )
    }
}

data class AdminAttendInfoDeleteResponse(
    val deletedScheduleCount: Int,
)
