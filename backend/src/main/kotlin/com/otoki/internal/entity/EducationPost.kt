package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 교육 게시물 Entity
 *
 * V1 테이블: education_mng
 * PK: edu_id (VARCHAR)
 */
@Entity
@Table(name = "education_mng")
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
    val empCode: String? = null,

    @Column(name = "inst_date")
    val instDate: LocalDateTime? = null,

    @Column(name = "upd_date")
    var updDate: LocalDateTime? = null

    // --- 주석 처리: V1 스키마에 없는 필드 ---
    // val id: Long = 0,                          // PK 변경: Long → String (eduId)
    // val category: EducationCategory,            // Enum → String (eduCode)
    // val title: String,                          // → eduTitle
    // val content: String,                        // → eduContent
    // @ManyToOne val createdBy: User,             // @ManyToOne → raw String (empCode)
    // val isActive: Boolean = true,               // V1에 없음
    // val createdAt: LocalDateTime,               // → instDate
    // var updatedAt: LocalDateTime                // → updDate
)
