package com.otoki.powersales.schedule.repository

import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
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

    fun existsByEmployeeAndWorkingDateAndWorkingType(employee: Employee, workingDate: LocalDate, workingType: String): Boolean

    fun existsByEmployeeAndAccountAndWorkingDateBetween(employee: Employee, account: Account, startDate: LocalDate, endDate: LocalDate): Boolean

    fun findByEmployeeAndAccountAndWorkingDate(employee: Employee, account: Account, workingDate: LocalDate): TeamMemberSchedule?
}
