package com.otoki.powersales.domain.activity.schedule.attendance

/**
 * 출근 등록(출근 마킹) 인터페이스
 *
 * 출근 등록의 본질은 `attendance_log` row 생성 + `team_member_schedule.attendance_log_id` 백링크다.
 * 레거시 `IF_REST_MOBILE_WorkReport` (SF 내부 `CommuteLog__c` insert + `TeamMemberSchedule__c` 연결) 에 대응한다.
 * 이 시점에는 SAP 등 외부 전송이 없으며, SAP 역전송은 별도 일배치 [AttendanceBatchService] 가 담당한다
 * (`attendance_log` 가 연결된 row 만 SAP 으로 전송 — 즉 본 등록이 그 대상 row 의 생산자).
 *
 * 인터페이스만 정의하고 Mock/실 구현을 분리한다.
 */
interface AttendanceRegistrar {

    /**
     * 출근 등록 (안전점검 데이터 포함)
     * @param request 안전점검 데이터 포함 출근 등록 요청
     * @return 출근 등록 결과
     */
    fun register(request: AttendanceRegisterRequest): AttendanceRegisterResult
}

/**
 * 출근 등록 결과
 */
data class AttendanceRegisterResult(
    val resultCode: String,
    val resultMessage: String
)
