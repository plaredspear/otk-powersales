package com.otoki.internal.repository

import com.otoki.internal.entity.SafetyCheckMemberId
import com.otoki.internal.entity.SafetyCheckSubmission
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyCheckSubmissionRepository : JpaRepository<SafetyCheckSubmission, SafetyCheckMemberId> {

    // Phase2: 기존 V2 필드(userId, submissionDate) 참조 메서드 주석 처리
    // fun findByUserIdAndSubmissionDate(userId: Long, submissionDate: LocalDate): Optional<SafetyCheckSubmission>
    // fun existsByUserIdAndSubmissionDate(userId: Long, submissionDate: LocalDate): Boolean
}
