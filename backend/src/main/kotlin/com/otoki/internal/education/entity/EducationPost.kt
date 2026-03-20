package com.otoki.internal.education.entity

import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*

/**
 * 교육 게시물 Entity
 *
 * V1 테이블: education_post (구: education_mng)
 * PK: edu_id (VARCHAR)
 */
@Entity
@Table(name = "education_post")
class EducationPost(

    @Id
    @Column(name = "edu_id", length = 20, nullable = false)
    val eduId: String,

    @Column(name = "edu_title", length = 150)
    val eduTitle: String? = null,

    @Column(name = "edu_content", columnDefinition = "TEXT")
    val eduContent: String? = null,

    @Column(name = "edu_code", length = 50)
    val eduCode: String? = null,

    @Column(name = "empcode__c", length = 40)
    val empCode: String? = null
) : BaseEntity()
