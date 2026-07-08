package com.otoki.powersales.domain.activity.schedule.attendance

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog

/**
 * 출근 등록(출근 마킹) 인터페이스
 *
 * 책임은 `attendance_log` row 생성 하나로 한정한다. `team_member_schedule.attendance_log_id` 백링크와
 * 안전점검 stamp 는 호출측([com.otoki.powersales.domain.activity.schedule.service.AttendanceService])이
 * managed entity 에 직접 반영한다 — 과거 registrar 가 bulk UPDATE 로 백링크를 세팅했을 때, 같은
 * 트랜잭션에서 뒤이어 dirty 가 된 entity 의 flush(전체 컬럼 UPDATE)가 stale null 로 백링크를 덮어써
 * 고아 출근로그 + 미등록 표시가 발생했다 (bulk DML 은 persistence context 를 우회하므로 managed entity 와
 * 같은 row 를 bulk 로 갱신하면 안 된다).
 *
 * 레거시 `IF_REST_MOBILE_WorkReport` (SF 내부 `CommuteLog__c` insert + `TeamMemberSchedule__c` 연결) 에 대응한다.
 * 이 시점에는 SAP 등 외부 전송이 없으며, SAP 역전송은 별도 일배치 [TeamMemberScheduleSapBatchService] 가 담당한다
 * (`attendance_log` 가 연결된 row 만 SAP 으로 전송 — 즉 본 등록이 그 대상 row 의 생산자).
 */
interface AttendanceRegistrar {

    /**
     * 출근로그 생성
     * @param request 출근로그 실체 데이터 (사원/거래처/출근종류/사유)
     * @return 저장된 attendance_log entity — 호출측이 TMS 백링크에 직접 연결한다
     */
    fun register(request: AttendanceRegisterRequest): AttendanceLog
}
