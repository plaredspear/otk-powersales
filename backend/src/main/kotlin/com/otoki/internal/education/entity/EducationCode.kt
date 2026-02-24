package com.otoki.internal.education.entity

import jakarta.persistence.*

/**
 * 교육 코드 Entity
 *
 * V1 테이블: education_code_mng
 * PK: edu_code (VARCHAR)
 */
@Entity
@Table(name = "education_code_mng")
class EducationCode(

    @Id
    @Column(name = "edu_code", length = 20, nullable = false)
    val eduCode: String,

    @Column(name = "edu_code_nm", length = 50)
    val eduCodeNm: String? = null,

    @Column(name = "edu_type", length = 10)
    val eduType: String? = null
)
