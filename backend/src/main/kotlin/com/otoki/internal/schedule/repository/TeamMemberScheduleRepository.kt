package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface TeamMemberScheduleRepository : JpaRepository<TeamMemberSchedule, Long>, TeamMemberScheduleRepositoryCustom {

    fun findByEmployeeIdAndWorkingDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun findByWorkingDateAndEmployeeIdIn(workingDate: LocalDate, employeeIds: List<Long>): List<TeamMemberSchedule>

    fun deleteAllByIdIn(ids: List<Long>)

    fun findByPromotionEmployeeIdIn(promotionEmployeeIds: List<Long>): List<TeamMemberSchedule>

    fun findByEmployeeIdInAndWorkingDateIn(employeeIds: List<Long>, workingDates: List<LocalDate>): List<TeamMemberSchedule>

    fun existsByEmployeeIdAndWorkingDateAndWorkingType(employeeId: Long, workingDate: LocalDate, workingType: String): Boolean
}
