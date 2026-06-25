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
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface TeamMemberScheduleRepository : JpaRepository<TeamMemberSchedule, Long>, TeamMemberScheduleRepositoryCustom {

    fun findByWorkingDateAndEmployeeIn(workingDate: LocalDate, employees: List<Employee>): List<TeamMemberSchedule>

    fun deleteAllByIdIn(ids: List<Long>)

    fun findByPromotionEmployeeIn(promotionEmployees: List<PromotionEmployee>): List<TeamMemberSchedule>

    fun findByEmployeeInAndWorkingDateIn(employees: List<Employee>, workingDates: List<LocalDate>): List<TeamMemberSchedule>

    fun existsByEmployeeAndWorkingDateAndWorkingType(employee: Employee, workingDate: LocalDate, workingType: WorkingType): Boolean

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
     * UC-06 진열마스터 단건 삭제 차단 — 레거시 SF lookup FK 매칭 동등.
     * `TeamMemberSchedule.displayWorkSchedule` (FK) 가 진열마스터를 가리키는 일정이 1건이라도 있으면 true.
     */
    fun existsByDisplayWorkSchedule(displayWorkSchedule: DisplayWorkSchedule): Boolean

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
     * 근무기간 조회 — 월별 개인 근무내역(어디서/어떻게) 조회.
     * `working_date asc, created_at asc` 정렬으로 일자 오름차순 캘린더/표 렌더에 적합.
     *
     * attendance_log_id 가 채워진(= 출근로그 연결된) 일정만 반환 — 사전 배정/행사/SAP 파생 등
     * 출근하지 않은 일정은 제외. 레거시 SF formula `isworkreport__c`(출퇴근 로그 존재 시 "근무등록")
     * 판별 기준과 동등하다.
     */
    fun findByEmployeeAndWorkingDateBetweenAndAttendanceLogIsNotNullOrderByWorkingDateAscCreatedAtAsc(
        employee: Employee,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<TeamMemberSchedule>
}
