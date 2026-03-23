package com.otoki.internal.education.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*

/**
 * 교육 첨부파일 Entity
 *
 * V1 테이블: education_post_attachment (구: education_file_mng, PK 없음 → @IdClass 복합 키)
 */
@Entity
@Table(name = "education_post_attachment")
@IdClass(EducationFileId::class)
@HCTable("education_file_mng")
class EducationPostAttachment(

    @Id
    @HCColumn("edu_id")
    @Column(name = "education_post_id", length = 20)
    val educationPostId: String,

    @Id
    @HCColumn("edu_file_key")
    @Column(name = "file_key", length = 30)
    val fileKey: String,

    @HCColumn("edu_file_type")
    @Column(name = "file_type", length = 10)
    val fileType: String? = null,

    @HCColumn("edu_file_orgnm")
    @Column(name = "file_original_name", length = 200)
    val fileOriginalName: String? = null
)
