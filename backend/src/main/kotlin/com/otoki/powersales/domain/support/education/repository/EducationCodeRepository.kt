package com.otoki.powersales.domain.support.education.repository

import com.otoki.powersales.domain.support.education.entity.EducationCode
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 코드 Repository
 */
interface EducationCodeRepository : JpaRepository<EducationCode, Long> {
    fun findByEduCode(eduCode: String): EducationCode?
    fun existsByEduCode(eduCode: String): Boolean
}
