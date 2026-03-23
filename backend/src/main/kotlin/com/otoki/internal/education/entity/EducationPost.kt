package com.otoki.internal.education.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * 교육 게시물 Entity
 *
 * V1 테이블: education_post (구: education_mng)
 * PK: education_post_id (VARCHAR, 구: edu_id)
 */
@Entity
@Table(name = "education_post")
@HCTable("education_mng")
class EducationPost(

    @Id
    @HCColumn("edu_id")
    @Column(name = "education_post_id", length = 20, nullable = false)
    val eduId: String,

    @HCColumn("edu_title")
    @Column(name = "title", length = 150)
    val eduTitle: String? = null,

    @HCColumn("edu_content")
    @Column(name = "content", columnDefinition = "TEXT")
    val eduContent: String? = null,

    @HCColumn("edu_code")
    @Column(name = "education_code", length = 50)
    val eduCode: String? = null,

    @HCColumn("empcode__c")
    @Column(name = "emp_code", length = 40)
    val empCode: String? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
