package com.otoki.powersales.domain.activity.safetycheck.repository

import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface SafetyCheckSubmissionRepository : JpaRepository<SafetyCheckSubmission, Long> {

    fun existsByEmployeeIdAndWorkingDate(employeeId: Long, workingDate: LocalDate): Boolean

    fun findByEmployeeIdAndWorkingDate(employeeId: Long, workingDate: LocalDate): Optional<SafetyCheckSubmission>

    fun findByEmployeeIdInAndWorkingDate(employeeIds: List<Long>, workingDate: LocalDate): List<SafetyCheckSubmission>
}
