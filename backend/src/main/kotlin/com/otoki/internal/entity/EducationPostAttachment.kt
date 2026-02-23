package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 교육 첨부파일 Entity
 *
 * V1 테이블: education_file_mng (PK 없음 → @IdClass 복합 키)
 */
@Entity
@Table(name = "education_file_mng")
@IdClass(EducationFileId::class)
class EducationPostAttachment(

    @Id
    @Column(name = "edu_id", length = 20)
    val eduId: String,

    @Id
    @Column(name = "edu_file_key", length = 30)
    val eduFileKey: String,

    @Column(name = "edu_file_type", length = 10)
    val eduFileType: String? = null,

    @Column(name = "edu_file_orgnm", length = 200)
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
