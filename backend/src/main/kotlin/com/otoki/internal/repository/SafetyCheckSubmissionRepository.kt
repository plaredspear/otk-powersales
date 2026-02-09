package com.otoki.internal.repository

import com.otoki.internal.entity.SafetyCheckSubmission
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface SafetyCheckSubmissionRepository : JpaRepository<SafetyCheckSubmission, Long> {

    fun findByUserIdAndSubmissionDate(userId: Long, submissionDate: LocalDate): Optional<SafetyCheckSubmission>

    fun existsByUserIdAndSubmissionDate(userId: Long, submissionDate: LocalDate): Boolean
}
