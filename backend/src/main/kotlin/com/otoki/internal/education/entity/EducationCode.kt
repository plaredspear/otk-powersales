package com.otoki.internal.education.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*

/**
 * 교육 코드 Entity
 *
 * V1 테이블: education_code (구: education_code_mng)
 * PK: education_code_id (VARCHAR, 구: edu_code)
 */
@Entity
@Table(name = "education_code")
@HCTable("education_code_mng")
class EducationCode(

    @Id
    @HCColumn("edu_code")
    @Column(name = "education_code_id", length = 20, nullable = false)
    val eduCode: String,

    @HCColumn("edu_code_nm")
    @Column(name = "edu_code_name", length = 50)
    val eduCodeNm: String? = null,

    @HCColumn("edu_type")
    @Column(name = "edu_type", length = 10)
    val eduType: String? = null
) : BaseEntity()
