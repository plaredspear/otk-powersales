package com.otoki.powersales.domain.support.education.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import com.otoki.powersales.employee.entity.Employee
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
@HerokuOnly("education_member_history")
class EducationViewHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_view_history_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_post_id")
    val educationPost: EducationPost? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @HCColumn("inst_date")
    @Column(name = "viewed_at", nullable = false)
    val viewedAt: LocalDateTime,

    @HCColumn("community_id")
    @Column(name = "edu_id", length = 20)
    val eduId: String? = null,

    @HCColumn("empcode__c")
    @Column(name = "emp_code", length = 40)
    val empCode: String? = null,

    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 40)
    val costCenterCode: String? = null
)
