package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationPost
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
