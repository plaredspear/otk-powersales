package com.otoki.internal.safetycheck.dto.response

import java.time.LocalDate
import java.time.LocalDateTime

data class SafetyCheckStatusResponse(
    val date: String,
    val totalCount: Int,
    val submittedCount: Int,
    val notSubmittedCount: Int,
    val members: List<MemberStatus>
)

data class MemberStatus(
    val id: Long,
    val employeeCode: String,
    val employeeName: String,
    val accountCode: String?,
    val accountName: String?,
    val submitted: Boolean,
    val submittedAt: LocalDateTime?,
    val startTime: LocalDateTime?,
    val equipments: List<EquipmentStatus>,
    val yesCount: Int,
    val noCount: Int,
    val precautions: String?,
    val precautionCount: Int,
    val workReportStatus: String?
)

data class EquipmentStatus(
    val seqNum: Int,
    val label: String,
    val answer: String
)
