package com.otoki.powersales.education.repository

import com.otoki.powersales.education.entity.EducationPost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EducationPostRepositoryCustom {

    fun findByEduCodeAndSearchWithPaging(
        eduCode: String,
        search: String,
        pageable: Pageable
    ): Page<EducationPost>

    fun findByOptionalEduCodeAndSearchWithPaging(
        eduCode: String?,
        search: String?,
        pageable: Pageable
    ): Page<EducationPost>
}
