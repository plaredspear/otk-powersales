package com.otoki.powersales.platform.common.dto.response

import java.time.LocalDateTime

/**
 * 홈 화면 통합 응답 DTO
 */
data class HomeResponse(
    val todaySchedules: List<TeamMemberScheduleInfo>,
    val attendanceSummary: AttendanceSummaryInfo,
    /**
     * 출근/근태 영역 노출 대상 여부.
     *
     * 레거시(home.jsp)는 `appauthority ∈ {여사원, 조장}` 일 때만 근태 영역을 렌더했다.
     * 지점장 / AccountViewAll / null(미매핑) 은 출근 영역 자체를 표시하지 않는다.
     * - 여사원: 본인 출근 등록
     * - 조장: 팀 출근 현황
     */
    val attendanceApplicable: Boolean,
    val safetyCheckRequired: Boolean,
    val expiryAlert: ExpiryAlertInfo?,
    val notices: List<NoticeInfo>,
    val currentDate: String
) {

    /**
     * 일정 정보 (역할별 스케줄 항목)
     */
    data class TeamMemberScheduleInfo(
        val scheduleId: Long,
        val displayWorkScheduleId: Long? = null,
        val employeeName: String,
        val employeeCode: String,
        val accountName: String?,
        val accountId: Long?,
        val workCategory: String,
        /**
         * 근무형태(고정/순회/격고). 레거시 home.jsp `workingcategory3__c` 정합.
         *
         * 순회/격고 근무자는 출근 등록 전에는 일정을 숨기므로(home.jsp:558·570),
         * 모바일 홈 카드가 이 값으로 표시 분기를 수행한다.
         */
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
        val employeeCode: String?,
        val expiryCount: Int
    )

    /**
     * 공지사항 정보
     */
    data class NoticeInfo(
        val id: Long,
        val title: String,
        val category: String,
        val categoryName: String,
        val createdAt: LocalDateTime
    )
}
