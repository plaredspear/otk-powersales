package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.QNotice.notice
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDateTime

class NoticeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : NoticeRepositoryCustom {

    override fun findNotices(
        category: String?,
        search: String?,
        branch: String,
        pageable: Pageable
    ): Page<Notice> {
        val where = BooleanBuilder()
            .and(buildDeletedCondition())
            .and(buildCategoryCondition(category, branch))
            .and(buildSearchCondition(search))

        val content = queryFactory
            .selectFrom(notice)
            .where(where)
            .orderBy(notice.createdDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(notice.count())
            .from(notice)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findRecentNotices(
        branch: String,
        since: LocalDateTime,
        branchCategory: String,
        allCategory: String
    ): List<Notice> {
        return queryFactory
            .selectFrom(notice)
            .where(
                notice.createdDate.goe(since),
                notice.category.eq(allCategory)
                    .or(notice.category.eq(branchCategory).and(notice.branch.eq(branch)))
            )
            .orderBy(notice.createdDate.desc())
            .limit(5)
            .fetch()
    }

    private fun buildDeletedCondition(): Predicate {
        return notice.isDeleted.isNull.or(notice.isDeleted.eq(false))
    }

    private fun buildCategoryCondition(category: String?, branch: String): Predicate {
        return when (category) {
            null -> notice.category.eq("ALL")
                .or(notice.category.eq("BRANCH").and(notice.branch.eq(branch)))
            "COMPANY" -> notice.category.eq("ALL")
            "BRANCH" -> notice.category.eq("BRANCH").and(notice.branch.eq(branch))
            else -> notice.category.eq(category)
        }
    }

    private fun buildSearchCondition(search: String?): Predicate? {
        if (search.isNullOrBlank()) return null
        val pattern = "%${search.lowercase()}%"
        return notice.name.lower().like(pattern)
            .or(notice.contents.lower().like(pattern))
    }
}
