package com.otoki.powersales.notice.repository

import com.otoki.powersales.notice.entity.Notice
import com.otoki.powersales.notice.enums.NoticeCategory
import com.otoki.powersales.notice.entity.QNotice.Companion.notice
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
class NoticeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : NoticeRepositoryCustom {

    override fun findNotices(
        category: NoticeCategory?,
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
            .orderBy(notice.createdAt.desc())
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

    override fun findAllNotices(
        category: NoticeCategory?,
        search: String?,
        pageable: Pageable
    ): Page<Notice> {
        val where = BooleanBuilder()
            .and(buildDeletedCondition())
            .and(buildAdminCategoryCondition(category))
            .and(buildSearchCondition(search))

        val content = queryFactory
            .selectFrom(notice)
            .where(where)
            .orderBy(notice.createdAt.desc())
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
        branch: String
    ): List<Notice> {
        return queryFactory
            .selectFrom(notice)
            .where(
                buildDeletedCondition(),
                notice.category.eq(NoticeCategory.COMPANY)
                    .or(notice.category.eq(NoticeCategory.BRANCH).and(notice.branch.eq(branch)))
                    .or(notice.category.eq(NoticeCategory.EDUCATION))
            )
            .orderBy(notice.createdAt.desc())
            .limit(5)
            .fetch()
    }

    private fun buildDeletedCondition(): Predicate {
        return notice.isDeleted.isNull.or(notice.isDeleted.eq(false))
    }

    private fun buildAdminCategoryCondition(category: NoticeCategory?): Predicate? {
        return when (category) {
            null -> null
            else -> notice.category.eq(category)
        }
    }

    private fun buildCategoryCondition(category: NoticeCategory?, branch: String): Predicate {
        return when (category) {
            null -> notice.category.eq(NoticeCategory.COMPANY)
                .or(notice.category.eq(NoticeCategory.BRANCH).and(notice.branch.eq(branch)))
            else -> {
                if (category == NoticeCategory.BRANCH) {
                    notice.category.eq(NoticeCategory.BRANCH).and(notice.branch.eq(branch))
                } else {
                    notice.category.eq(category)
                }
            }
        }
    }

    private fun buildSearchCondition(search: String?): Predicate? {
        if (search.isNullOrBlank()) return null
        val pattern = "%${search.lowercase()}%"
        return notice.name.lower().like(pattern)
            .or(notice.contents.lower().like(pattern))
    }
}
