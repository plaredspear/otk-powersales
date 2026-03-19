package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface TeamMemberScheduleRepository : JpaRepository<TeamMemberSchedule, Long>, TeamMemberScheduleRepositoryCustom {

    fun findByEmployeeNumberAndWorkingDate(employeeNumber: String, workingDate: LocalDate): List<TeamMemberSchedule>

    fun findByWorkingDateAndEmployeeNumberIn(workingDate: LocalDate, employeeNumbers: List<String>): List<TeamMemberSchedule>

    fun deleteAllByIdIn(ids: List<Long>)

    fun findByPromotionEmpIdExtIn(promotionEmpIdExts: List<String>): List<TeamMemberSchedule>

    fun findByEmployeeNumberInAndWorkingDateIn(employeeNumbers: List<String>, workingDates: List<LocalDate>): List<TeamMemberSchedule>

    fun existsByEmployeeNumberAndWorkingDateAndWorkingType(employeeNumber: String, workingDate: LocalDate, workingType: String): Boolean
}
