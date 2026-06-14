package com.otoki.powersales.domain.support.education.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*

/**
 * 교육 코드 Entity
 *
 * V1 테이블: education_code (구: education_code_mng)
 * PK: education_code_id (BIGINT IDENTITY)
 */
@Entity
@Table(name = "education_code")
@HerokuOnly("education_code_mng")
class EducationCode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_code_id")
    val id: Long = 0,

    @HCColumn("edu_code")
    @Column(name = "edu_code", length = 20, nullable = false, unique = true)
    val eduCode: String,

    @HCColumn("edu_code_nm")
    @Column(name = "edu_code_name", length = 50)
    val eduCodeNm: String? = null,

    @HCColumn("edu_type")
    @Column(name = "edu_type", length = 10)
    val eduType: String? = null
) : BaseEntity()
