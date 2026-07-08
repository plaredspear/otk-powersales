package com.otoki.powersales.domain.activity.schedule.attendance

import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType

/**
 * 출근로그 생성 요청 데이터
 *
 * `attendance_log` row 에 실릴 실체 데이터([employeeId] / [accountId] / [attendanceType] / [reason])만 전달한다.
 * 레거시 SF `IF_REST_MOBILE_WorkReport` 는 CommuteLog__c 에 `CommuteDate__c`(=서버 now) 와
 * `Reason__c` 만 채우고 사원/거래처는 TeamMemberSchedule 측에 두었으나, 신규 시스템은 id-FK 모델이라
 * 출근로그 자체에 사원/거래처 id 를 적재해 admin 출근현황 조회(AdminAttendanceLog*)와 정합시킨다.
 *
 * TMS 백링크·안전점검 stamp 는 registrar 책임이 아니므로 스케줄/안전점검 필드를 갖지 않는다
 * ([AttendanceRegistrar] 문서 참조).
 */
data class AttendanceRegisterRequest(
    val employeeId: Long? = null,
    val accountId: Long? = null,
    val attendanceType: AttendanceType = AttendanceType.REGULAR,
    val reason: String? = null,
)
