package com.otoki.internal.dto.response

import java.time.LocalDateTime

/**
 * 홈 화면 통합 응답 DTO
 */
data class HomeResponse(
    val todaySchedules: List<ScheduleInfo>,
    val expiryAlert: ExpiryAlertInfo?,
    val notices: List<NoticeInfo>,
    val currentDate: String
) {

    /**
     * 일정 정보
     */
    data class ScheduleInfo(
        val id: Long,
        val storeName: String,
        val startTime: String,
        val endTime: String,
        val type: String
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
