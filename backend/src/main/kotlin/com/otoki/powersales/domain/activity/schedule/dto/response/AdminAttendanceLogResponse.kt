package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import java.time.LocalDateTime

data class AdminAttendanceLogListItemResponse(
    val id: Long,
    val name: String?,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val employeeJobCode: String?,
    val accountId: Long?,
    val accountCode: String?,
    val accountName: String?,
    val attendanceDate: LocalDateTime?,
    val attendanceType: AttendanceType?,
    val secondWorkType: SecondWorkType?,
    val secondWorkTypeName: String?,
    val reason: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(entity: AttendanceLog): AdminAttendanceLogListItemResponse {
            val emp = entity.employee
            val acc = entity.account
            return AdminAttendanceLogListItemResponse(
                id = entity.id,
                name = entity.name,
                employeeId = entity.employeeId,
                employeeCode = emp?.employeeCode,
                employeeName = emp?.name,
                employeeJobCode = emp?.jobCode,
                accountId = entity.accountId,
                accountCode = acc?.employeeCode,
                accountName = acc?.name,
                attendanceDate = entity.attendanceDate,
                attendanceType = entity.attendanceType,
                secondWorkType = entity.secondWorkType,
                secondWorkTypeName = entity.secondWorkType?.displayName,
                reason = entity.reason,
                createdAt = entity.createdAt,
            )
        }
    }
}

data class AdminAttendanceLogDetailResponse(
    val id: Long,
    val name: String?,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val employeeJobCode: String?,
    val accountId: Long?,
    val accountCode: String?,
    val accountName: String?,
    val attendanceDate: LocalDateTime?,
    val attendanceType: AttendanceType?,
    val secondWorkType: SecondWorkType?,
    val secondWorkTypeName: String?,
    val reason: String?,
    val ownerUserName: String?,
    val createdByName: String?,
    val lastModifiedByName: String?,
    val isDeleted: Boolean?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        // sfid 는 SF 데이터 마이그레이션 보조 필드 — API 응답에 노출 금지 (정책).
        fun from(entity: AttendanceLog): AdminAttendanceLogDetailResponse {
            val emp = entity.employee
            val acc = entity.account
            return AdminAttendanceLogDetailResponse(
                id = entity.id,
                name = entity.name,
                employeeId = entity.employeeId,
                employeeCode = emp?.employeeCode,
                employeeName = emp?.name,
                employeeJobCode = emp?.jobCode,
                accountId = entity.accountId,
                accountCode = acc?.employeeCode,
                accountName = acc?.name,
                attendanceDate = entity.attendanceDate,
                attendanceType = entity.attendanceType,
                secondWorkType = entity.secondWorkType,
                secondWorkTypeName = entity.secondWorkType?.displayName,
                reason = entity.reason,
                ownerUserName = entity.ownerUser?.name,
                createdByName = entity.createdBy?.name,
                lastModifiedByName = entity.lastModifiedBy?.name,
                isDeleted = entity.isDeleted,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }
}
