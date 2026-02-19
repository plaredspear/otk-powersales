package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "safety_check_submissions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_safety_check_user_date",
            columnNames = ["user_id", "submission_date"]
        )
    ]
)
class SafetyCheckSubmission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "submission_date", nullable = false)
    val submissionDate: LocalDate,

    @Column(name = "submitted_at", nullable = false)
    val submittedAt: LocalDateTime = LocalDateTime.now(),

    // Phase2: SafetyCheckSubmissionItem 주석 처리로 관계 제거
    // @OneToMany(mappedBy = "submission", cascade = [CascadeType.ALL], orphanRemoval = true)
    // val submissionItems: MutableList<SafetyCheckSubmissionItem> = mutableListOf()
) {
    // Phase2: addItem 비활성화
    fun addItem(item: SafetyCheckItem) {
        // no-op: SafetyCheckSubmissionItem 주석 처리됨
    }
}
