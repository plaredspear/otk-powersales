package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.schedule.entity.AttendanceLog
import com.otoki.powersales.schedule.enums.AttendanceType
import com.otoki.powersales.schedule.enums.SecondWorkType
import java.time.LocalDateTime

data class AdminAttendanceLogListItemResponse(
    val id: Long,
    val name: String?,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val employeeJobCode: String?,
    val accountId: Int?,
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
    val sfid: String?,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val employeeJobCode: String?,
    val employeeSfid: String?,
    val accountId: Int?,
    val accountCode: String?,
    val accountName: String?,
    val accountSfid: String?,
    val attendanceDate: LocalDateTime?,
    val attendanceType: AttendanceType?,
    val secondWorkType: SecondWorkType?,
    val secondWorkTypeName: String?,
    val reason: String?,
    val ownerSfid: String?,
    val ownerUserName: String?,
    val createdBySfid: String?,
    val createdByName: String?,
    val lastModifiedBySfid: String?,
    val lastModifiedByName: String?,
    val isDeleted: Boolean?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(entity: AttendanceLog): AdminAttendanceLogDetailResponse {
            val emp = entity.employee
            val acc = entity.account
            return AdminAttendanceLogDetailResponse(
                id = entity.id,
                name = entity.name,
                sfid = entity.sfid,
                employeeId = entity.employeeId,
                employeeCode = emp?.employeeCode,
                employeeName = emp?.name,
                employeeJobCode = emp?.jobCode,
                employeeSfid = entity.employeeSfid,
                accountId = entity.accountId,
                accountCode = acc?.employeeCode,
                accountName = acc?.name,
                accountSfid = entity.accountSfid,
                attendanceDate = entity.attendanceDate,
                attendanceType = entity.attendanceType,
                secondWorkType = entity.secondWorkType,
                secondWorkTypeName = entity.secondWorkType?.displayName,
                reason = entity.reason,
                ownerSfid = entity.ownerSfid,
                ownerUserName = entity.ownerUser?.name,
                createdBySfid = entity.createdBySfid,
                createdByName = entity.createdBy?.name,
                lastModifiedBySfid = entity.lastModifiedBySfid,
                lastModifiedByName = entity.lastModifiedBy?.name,
                isDeleted = entity.isDeleted,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }
}
