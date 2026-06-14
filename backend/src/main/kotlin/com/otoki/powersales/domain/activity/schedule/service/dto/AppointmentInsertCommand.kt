package com.otoki.powersales.domain.activity.schedule.service.dto

/**
 * 인사발령 INSERT 도메인 입력 커맨드.
 *
 * INSERT only — 멱등성 미보장 (레거시 동등). 멱등성 강화는 후속 스펙 #567.
 */
data class AppointmentInsertCommand(
    val employeeCode: String?,
    val afterOrgCode: String?,
    val afterOrgName: String?,
    val jikchak: String?,
    val jikwee: String?,
    val jikgub: String?,
    val workType: String?,
    val manageType: String?,
    val jobCode: String?,
    val workArea: String?,
    val jikjong: String?,
    val appointDate: String?,
    val jobName: String?,
    val ordDetailCode: String?,
    val ordDetailNode: String?
)
