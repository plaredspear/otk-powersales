package com.otoki.powersales.schedule.service.dto

/**
 * 출근 정보 INSERT 도메인 입력 커맨드 (단일 청크 단위).
 *
 * INSERT only — 멱등성 미보장 (레거시 동등). 청크 분할은 어댑터 책임.
 */
data class AttendInfoInsertCommand(
    val employeeCode: String?,
    val startDate: String?,
    val endDate: String?,
    val attendType: String?,
    val status: String?
)
