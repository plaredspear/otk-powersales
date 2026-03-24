package com.otoki.internal.education.entity

import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.sap.entity.Employee
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 교육 조회 이력 Entity
 *
 * V1 테이블: education_view_history (구: education_member_history)
 * V39: 복합키 → IDENTITY PK + FK 정규화
 */
@Entity
@Table(name = "education_view_history")
@HCTable("education_member_history")
class EducationViewHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_view_history_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_post_id", nullable = false)
    val educationPost: EducationPost,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,

    @Column(name = "viewed_at", nullable = false)
    val viewedAt: LocalDateTime
)
