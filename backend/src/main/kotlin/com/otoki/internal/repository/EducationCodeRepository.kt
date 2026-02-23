package com.otoki.internal.repository

import com.otoki.internal.entity.EducationCode
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 코드 Repository
 */
interface EducationCodeRepository : JpaRepository<EducationCode, String>
