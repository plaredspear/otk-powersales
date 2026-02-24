package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationCode
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 코드 Repository
 */
interface EducationCodeRepository : JpaRepository<EducationCode, String>
