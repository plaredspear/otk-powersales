package com.otoki.powersales.schedule.repository

import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
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
     * Spec #587 P1-B §1.2 step 4 — 동일 `(employee, working_date, working_category3)` 조합 중복 검증.
     * 진열 출근 시 다른 거래처라도 같은 근무유형(고정/격고/순회)으로 이미 일정이 있으면 거부.
     */
    fun existsByEmployeeAndWorkingDateAndWorkingCategory3(
        employee: Employee,
        workingDate: LocalDate,
        workingCategory3: WorkingCategory3
    ): Boolean

    /**
     * 여사원 상세 — 시간순서별 근무이력 조회.
     * `working_date desc, created_at desc` 정렬. limit 는 호출처 `Pageable` 로 제어.
     */
    fun findByEmployeeOrderByWorkingDateDescCreatedAtDesc(
        employee: Employee,
        pageable: Pageable,
    ): List<TeamMemberSchedule>
}
