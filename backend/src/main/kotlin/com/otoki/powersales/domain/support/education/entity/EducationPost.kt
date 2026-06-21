package com.otoki.powersales.domain.support.education.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import com.otoki.powersales.domain.org.employee.entity.Employee
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
/**
 * 교육 게시물 Entity
 *
 * V1 테이블: education_post (구: education_mng)
 * PK: education_post_id (BIGINT IDENTITY)
 */
@DomainName("교육게시물")
@Entity
@Table(name = "education_post")
@HerokuOnly("education_mng")
class EducationPost(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_post_id", nullable = false)
    val id: Long = 0,

    @HCColumn("edu_id")
    @Column(name = "edu_id", length = 20)
    val eduId: String? = null,

    @HCColumn("edu_title")
    @Column(name = "title", length = 150)
    val eduTitle: String? = null,

    @HCColumn("edu_content")
    @Column(name = "content", columnDefinition = "TEXT")
    val eduContent: String? = null,

    @HCColumn("edu_code")
    @Column(name = "education_code", length = 50)
    val eduCode: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @HCColumn("empcode__c")
    @Column(name = "emp_code", length = 40)
    val empCode: String? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()
