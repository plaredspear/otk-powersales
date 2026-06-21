package com.otoki.powersales.domain.support.education.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import com.otoki.powersales.domain.org.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 교육 조회 이력 Entity
 *
 * V1 테이블: education_view_history (구: education_member_history)
 * V39: 복합키 → IDENTITY PK + FK 정규화
 */
@DomainName("교육조회이력")
@Entity
@Table(name = "education_view_history")
@HerokuOnly("education_member_history")
class EducationViewHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("교육조회이력ID")
    @Column(name = "education_view_history_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_post_id")
    val educationPost: EducationPost? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @HCColumn("inst_date")
    @FieldName("조회일시")
    @Column(name = "viewed_at", nullable = false)
    val viewedAt: LocalDateTime,

    @HCColumn("community_id")
    @FieldName("교육ID")
    @Column(name = "edu_id", length = 20)
    val eduId: String? = null,

    @HCColumn("empcode__c")
    @FieldName("사번")
    @Column(name = "emp_code", length = 40)
    val empCode: String? = null,

    @HCColumn("costcentercode__c")
    @FieldName("조직유형")
    @Column(name = "cost_center_code", length = 40)
    val costCenterCode: String? = null
)
