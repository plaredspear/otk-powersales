package com.otoki.powersales.domain.support.education.repository

import com.otoki.powersales.domain.support.education.entity.EducationPost
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
