package com.otoki.powersales.domain.support.education.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * 교육 첨부파일 Entity
 *
 * V1 테이블: education_post_attachment (구: education_file_mng)
 * PK: education_post_attachment_id (BIGINT IDENTITY)
 */
@DomainName("교육첨부파일")
@Entity
@Table(name = "education_post_attachment")
@HerokuOnly("education_file_mng")
class EducationPostAttachment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_post_attachment_id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "education_post_id")
    val educationPost: EducationPost? = null,

    @HCColumn("edu_id")
    @Column(name = "edu_id", length = 20)
    val eduId: String? = null,

    @HCColumn("edu_file_key")
    @Column(name = "file_key", length = 30, nullable = false)
    val fileKey: String,

    @HCColumn("edu_file_type")
    @Column(name = "file_type", length = 10)
    val fileType: String? = null,

    @HCColumn("edu_file_orgnm")
    @Column(name = "file_original_name", length = 200)
    val fileOriginalName: String? = null
)
