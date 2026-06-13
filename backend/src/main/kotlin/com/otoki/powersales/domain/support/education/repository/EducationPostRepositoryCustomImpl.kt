package com.otoki.powersales.domain.support.education.repository

import com.otoki.powersales.domain.support.education.entity.EducationPost
import com.otoki.powersales.domain.support.education.entity.QEducationPost.Companion.educationPost
import com.querydsl.core.BooleanBuilder
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
            .orderBy(educationPost.createdAt.desc())
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

    override fun findByOptionalEduCodeAndSearchWithPaging(
        eduCode: String?,
        search: String?,
        pageable: Pageable
    ): Page<EducationPost> {
        val where = BooleanBuilder()

        if (!eduCode.isNullOrBlank()) {
            where.and(educationPost.eduCode.eq(eduCode))
        }

        if (!search.isNullOrBlank()) {
            val pattern = "%${search.take(100)}%"
            where.and(
                educationPost.eduTitle.like(pattern)
                    .or(educationPost.eduContent.like(pattern))
            )
        }

        val content = queryFactory
            .selectFrom(educationPost)
            .where(where)
            .orderBy(educationPost.createdAt.desc())
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
