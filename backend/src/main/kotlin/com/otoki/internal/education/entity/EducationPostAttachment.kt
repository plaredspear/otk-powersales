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
    val eduId: String,

    @Id
    @HCColumn("edu_file_key")
    @Column(name = "file_key", length = 30)
    val eduFileKey: String,

    @HCColumn("edu_file_type")
    @Column(name = "file_type", length = 10)
    val eduFileType: String? = null,

    @HCColumn("edu_file_orgnm")
    @Column(name = "file_original_name", length = 200)
    val eduFileOrgNm: String? = null

    // --- 주석 처리: V1 스키마에 없는 필드 ---
    // val id: Long = 0,                          // PK 변경 → @IdClass
    // @ManyToOne val post: EducationPost,         // @ManyToOne → raw String (eduId)
    // val fileName: String,                       // → eduFileOrgNm
    // val fileUrl: String,                        // → eduFileKey
    // val fileSize: Long,                         // V1에 없음
    // val contentType: String,                    // → eduFileType
    // val createdAt: LocalDateTime                // V1에 없음
)
