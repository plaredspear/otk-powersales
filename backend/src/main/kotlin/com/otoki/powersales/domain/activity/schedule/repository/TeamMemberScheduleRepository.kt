package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface TeamMemberScheduleRepository : JpaRepository<TeamMemberSchedule, Long>, TeamMemberScheduleRepositoryCustom {

    /**
     * 여사원 현황 "근무형태" 필터 — 사원별 **가장 최근 출근등록 1건**의 근무형태(1/3)가 조건과 일치하는
     * employee_id 집합. PostgreSQL `DISTINCT ON` 으로 각 사원의 최근 1건을 인덱스
     * (`idx_team_member_schedule_employee_id_working_date`) 단일 스캔으로 뽑는다.
     *
     * 기존에 애플리케이션 레이어에서 (1) 사원별 MAX(working_date) → (2) `employee_id IN (전 사원)
     * AND working_date IN (전 최근일자)` 2쿼리로 처리하던 방식은, 전 사원 대상일 때 두 IN 의 곱집합이
     * 폭발해 timeout 이 발생했다. DB 단일 쿼리로 대체한다.
     *
     * ## 왜 employee 목록 쿼리의 상관 서브쿼리로 되돌리지 않는가 (실측 근거)
     * 더 단순한 대안은 employee 목록 쿼리 WHERE 에 `EXISTS(최근 1건이 조건 일치)` 상관 서브쿼리를 붙이는
     * 것이다. 본 인덱스가 있으면 **목록 조회(LIMIT 20)는 24ms** 로 빠르다 — LIMIT 이 상관 서브쿼리를
     * 조기 종료시키기 때문. 그러나 페이징 **count 쿼리(totalElements)는 LIMIT 이 없어 전 사원에 대해
     * 상관 서브쿼리를 전건 평가**해야 하고, 옵티마이저가 이를 Hash Anti Join 으로 풀며 ~1.9억 행을 비교해
     * **32초 (timeout)** 가 걸린다. 화면은 목록+count 를 함께 호출하므로 이 방식으로는 근본 해결이 안 된다.
     * 반면 본 DISTINCT ON 방식은 team_member_schedule 을 **한 번만** index-only scan 하여 목록·count 와
     * 무관하게 ~0.3초로 끝난다 (dev 실측). 그래서 목록/count 를 employee 쿼리에서 분리하고 매칭 집합만
     * 여기서 산출한다.
     *
     * '최근 1건' tie-break: working_date DESC, team_member_schedule_id DESC (같은 날이면 마지막 등록).
     * 근무형태 컬럼은 converter 로 displayName("진열"/"행사", "고정"/"격고"/"순회") 문자열이 저장된다.
     *
     * @param workType1 근무형태1 displayName. null 이면 조건 미적용.
     * @param workType3 근무형태3 displayName. null 이면 조건 미적용.
     */
    @Query(
        nativeQuery = true,
        value = """
            SELECT latest.employee_id
            FROM (
                SELECT DISTINCT ON (tms.employee_id)
                       tms.employee_id       AS employee_id,
                       tms.working_category1 AS working_category1,
                       tms.working_category3 AS working_category3
                FROM powersales.team_member_schedule tms
                WHERE tms.attendance_log_id IS NOT NULL
                ORDER BY tms.employee_id, tms.working_date DESC, tms.team_member_schedule_id DESC
            ) latest
            WHERE (CAST(:workType1 AS text) IS NULL OR latest.working_category1 = :workType1)
              AND (CAST(:workType3 AS text) IS NULL OR latest.working_category3 = :workType3)
        """,
    )
    fun findEmployeeIdsByLatestWorkType(
        @Param("workType1") workType1: String?,
        @Param("workType3") workType3: String?,
    ): List<Long>

    fun findByWorkingDateAndEmployeeIn(workingDate: LocalDate, employees: List<Employee>): List<TeamMemberSchedule>

    fun deleteAllByIdIn(ids: List<Long>)

    fun findByPromotionEmployeeIn(promotionEmployees: List<PromotionEmployee>): List<TeamMemberSchedule>

    fun findByEmployeeInAndWorkingDateIn(employees: List<Employee>, workingDates: List<LocalDate>): List<TeamMemberSchedule>

    fun existsByEmployeeAndWorkingDateAndWorkingType(employee: Employee, workingDate: LocalDate, workingType: WorkingType): Boolean

    /**
     * Spec #553 - SAP attend_info Status='N' 분기 멱등 처리용 단건 조회.
     * `(employee, working_date, working_type='연차')` 응용 레벨 유일 키. 이미 존재하는 연차 일정을
     * 최신 AttendInfo 로 재링크(attend_info FK 갱신)할 때 사용한다.
     */
    fun findByEmployeeAndWorkingDateAndWorkingType(employee: Employee, workingDate: LocalDate, workingType: WorkingType): TeamMemberSchedule?

    /**
     * Spec #553 - SAP attend_info Status='Y' 분기에서 동일 직원·기간·workingType 일정 일괄 삭제 대상 조회.
     */
    fun findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
        employee: Employee,
        startDate: LocalDate,
        endDate: LocalDate,
        workingType: WorkingType
    ): List<TeamMemberSchedule>

    /**
     * 진열마스터 수정/삭제 차단 — 연결 여사원일정 중 **근무등록(출근보고)이 시작된 건**
     * (출근보고시각 `commuteReportDatetime` 채워짐)이 1건이라도 있으면 true.
     * 단순 FK 매칭(편성만 되고 미출근인 연결)과 구분하기 위해 출근보고시각 NOT NULL 조건 사용.
     * SF 의 출근(commute) semantics (`CommuteReportDateTime__c`/`CommuteLogId__c` 채워짐) 정합.
     * 근무등록이 시작되지 않았다면 확정여부와 무관하게 수정/삭제를 허용한다.
     */
    fun existsByDisplayWorkScheduleAndCommuteReportDatetimeIsNotNull(displayWorkSchedule: DisplayWorkSchedule): Boolean

    /** 진열 마스터 삭제 시 연결 TMS 의 FK SetNull 처리용 (SF deleteConstraint=SetNull 동등). */
    fun findByDisplayWorkSchedule(displayWorkSchedule: DisplayWorkSchedule): List<TeamMemberSchedule>

    fun findByEmployeeAndAccountAndWorkingDate(employee: Employee, account: Account, workingDate: LocalDate): TeamMemberSchedule?

    /**
     * 진열 출근 중복 검증 — 동일 `(employee, working_date, working_category3)` 조합 일정 건수.
     * 레거시 `TeamMemberScheduleTriggerHandler.checkDuplicatedSchedule` 의 AggregateResult
     * (사원+날짜+근무유형3 GROUP BY COUNT) 동등. 거래처/출근여부 무관, 일정 존재 기준 카운트.
     * 유형별 양립 매트릭스(고정/격고/순회)는 호출처(AttendanceService)가 이 카운트로 판정.
     */
    fun countByEmployeeAndWorkingDateAndWorkingCategory3(
        employee: Employee,
        workingDate: LocalDate,
        workingCategory3: WorkingCategory3
    ): Long

    /**
     * 여사원 상세 — 시간순서별 근무이력 조회.
     * `working_date desc, created_at desc` 정렬. limit 는 호출처 `Pageable` 로 제어.
     */
    fun findByEmployeeOrderByWorkingDateDescCreatedAtDesc(
        employee: Employee,
        pageable: Pageable,
    ): List<TeamMemberSchedule>

    /**
     * team_member_schedule.name 채번 — SF AutoNumber(Name, "TS{00000000}") 재현. 시퀀스 nextval 단독.
     *
     * 시퀀스를 기존 데이터 최대 번호 위로 끌어올리는 MAX 보정은 [syncNameSeq] 가 담당하며,
     * **번호가 외부에서 주입될 수 있는 시점에만** 실행한다 (부팅 1회 / SF 마이그레이션 직후 —
     * `NameSequenceSyncService`). 상시 운영 중에는 앱 밖에서 이 테이블에 name 을 넣는 경로가 없으므로
     * (SAP 인바운드·배치 모두 본 채번을 경유) 채번마다 MAX 를 재확인할 이유가 없다.
     * 과거에는 채번마다 `GREATEST(nextval, MAX+1)` 로 보정했는데, MAX 대상이 표현식이라 채번 1회가
     * 1.76M row 테이블 전건 스캔이었다 (행사 확정 지연의 주원인).
     */
    @Query(
        value = "SELECT nextval('powersales.team_member_schedule_name_seq')",
        nativeQuery = true
    )
    fun getNextNameSeq(): Long

    /**
     * team_member_schedule.name 벌크 채번 — [getNextNameSeq] 를 [count] 건만큼 반복한 것과 동일한
     * 번호 구간을 **쿼리 1회**로 확보한다 (행사 확정 / 연차 전개처럼 N 건을 한 번에 만드는 경로용).
     *
     * 반환값은 **구간의 마지막 번호**이며, 발급 구간은 `[반환값 - count + 1, 반환값]` 이다.
     * nextval 로 구간 시작을 원자적으로 확보한 뒤 setval 로 구간 끝까지 밀어두므로 동시 요청과 번호가
     * 겹치지 않는다 (건별 호출과 동일 보장).
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.team_member_schedule_name_seq',
                nextval('powersales.team_member_schedule_name_seq') + (:count - 1)
            )
        """,
        nativeQuery = true
    )
    fun allocateNameSeqBlock(@Param("count") count: Long): Long

    /**
     * name 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     *
     * SF 마이그레이션(Stage1 COPY)은 SF 원본 Name 을 그대로 적재하므로 시퀀스가 뒤처질 수 있다.
     * 이미 앞서 있으면 nextval 1개만 소모하고 값은 그대로다. MAX 대상 표현식에는 부분 인덱스
     * (`idx_tms_name_seq_num`)가 있어 index scan 으로 처리된다.
     * 호출 지점은 `NameSequenceSyncService` 참조 — 채번 hot path 에서는 호출하지 않는다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.team_member_schedule_name_seq',
                GREATEST(
                    nextval('powersales.team_member_schedule_name_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
                           FROM powersales.team_member_schedule
                          WHERE name ~ '^TS[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long

    /** name 이 비어(NULL/공백) 채번이 필요한 일정 건수 — 백필 도구 preview 용. */
    @Query(
        value = "SELECT COUNT(*) FROM powersales.team_member_schedule WHERE name IS NULL OR name = ''",
        nativeQuery = true
    )
    fun countMissingName(): Long

    /**
     * name 이 비어 채번이 필요한 일정 id 를 오래된 순(id ASC)으로 상한(limit)만큼 조회 — 백필 도구용.
     * 채번은 호출부가 id 별로 [getNextNameSeq] 를 돌려 [updateNameById] 로 반영한다.
     */
    @Query(
        value = """
            SELECT team_member_schedule_id
              FROM powersales.team_member_schedule
             WHERE name IS NULL OR name = ''
             ORDER BY team_member_schedule_id ASC
             LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findMissingNameIds(@Param("limit") limit: Int): List<Long>

    /**
     * 백필 도구 전용 — 단건 name 을 native UPDATE (name 은 entity 에서 val 이라 JPA dirty update 불가).
     * @LastModifiedDate 등 audit 컬럼은 건드리지 않는다 (name 만 채우는 데이터 정합 목적).
     * @return 갱신 row 수 (1 이면 성공, 0 이면 id 부재 또는 그새 name 이 채워짐).
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(
        value = """
            UPDATE powersales.team_member_schedule
               SET name = :name
             WHERE team_member_schedule_id = :id
               AND (name IS NULL OR name = '')
        """,
        nativeQuery = true
    )
    fun updateNameById(@Param("id") id: Long, @Param("name") name: String): Int
}
