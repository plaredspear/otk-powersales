package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationPost
import com.otoki.internal.education.entity.QEducationPost.educationPost
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class EducationPostRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : EducationPostRepositoryCustom {

    override fun findByEduCodeAndSearchWithPaging(
        eduCode: String,
        search: String,
        pageable: Pageable
    ): Page<EducationPost> {
        val pattern = "%$search%"

        val where = educationPost.eduCode.eq(eduCode)
            .and(
                educationPost.eduTitle.like(pattern)
                    .or(educationPost.eduContent.like(pattern))
            )

        val content = queryFactory
            .selectFrom(educationPost)
            .where(where)
            .orderBy(educationPost.instDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(educationPost.count())
            .from(educationPost)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
