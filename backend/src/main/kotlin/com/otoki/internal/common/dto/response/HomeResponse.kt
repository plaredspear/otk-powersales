package com.otoki.internal.common.dto.response

import java.time.LocalDateTime

/**
 * 홈 화면 통합 응답 DTO
 */
data class HomeResponse(
    val todaySchedules: List<ScheduleInfo>,
    val attendanceSummary: AttendanceSummaryInfo,
    val safetyCheckRequired: Boolean,
    val expiryAlert: ExpiryAlertInfo?,
    val notices: List<NoticeInfo>,
    val currentDate: String
) {

    /**
     * 일정 정보 (역할별 스케줄 항목)
     */
    data class ScheduleInfo(
        val scheduleId: String,
        val employeeName: String,
        val employeeSfid: String,
        val storeName: String?,
        val storeSfid: String?,
        val workCategory: String,
        val workType: String?,
        val isCommuteRegistered: Boolean,
        val commuteRegisteredAt: LocalDateTime?
    )

    /**
     * 출근 현황 집계
     */
    data class AttendanceSummaryInfo(
        val totalCount: Int,
        val registeredCount: Int
    )

    /**
     * 유통기한 알림 정보
     */
    data class ExpiryAlertInfo(
        val branchName: String,
        val employeeName: String,
        val employeeId: String,
        val expiryCount: Int
    )

    /**
     * 공지사항 정보
     */
    data class NoticeInfo(
        val id: Long,
        val title: String,
        val type: String,
        val createdAt: LocalDateTime
    )
}
