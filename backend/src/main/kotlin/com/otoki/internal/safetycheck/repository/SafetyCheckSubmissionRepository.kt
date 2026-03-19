package com.otoki.internal.safetycheck.repository

import com.otoki.internal.safetycheck.entity.SafetyCheckMemberId
import com.otoki.internal.safetycheck.entity.SafetyCheckSubmission
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface SafetyCheckSubmissionRepository : JpaRepository<SafetyCheckSubmission, SafetyCheckMemberId> {

    fun existsByEmployeeNumberAndWorkingDate(employeeNumber: String, workingDate: LocalDate): Boolean

    fun findByEmployeeNumberAndWorkingDate(employeeNumber: String, workingDate: LocalDate): Optional<SafetyCheckSubmission>

    fun findByEmployeeNumberInAndWorkingDate(employeeNumbers: List<String>, workingDate: LocalDate): List<SafetyCheckSubmission>
}
