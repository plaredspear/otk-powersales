package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface TeamMemberScheduleRepository : JpaRepository<TeamMemberSchedule, Long>, TeamMemberScheduleRepositoryCustom {

    fun findByEmployeeIdAndWorkingDate(employeeId: String, workingDate: LocalDate): List<TeamMemberSchedule>

    fun findByWorkingDateAndEmployeeIdIn(workingDate: LocalDate, employeeIds: List<String>): List<TeamMemberSchedule>

    fun deleteAllByIdIn(ids: List<Long>)

    fun findByPromotionEmpIdExtIn(promotionEmpIdExts: List<String>): List<TeamMemberSchedule>

    fun findByEmployeeIdInAndWorkingDateIn(employeeIds: List<String>, workingDates: List<LocalDate>): List<TeamMemberSchedule>

    fun existsByEmployeeIdAndWorkingDateAndWorkingType(employeeId: String, workingDate: LocalDate, workingType: String): Boolean
}
