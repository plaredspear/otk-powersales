package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.entity.QInspectionTheme.Companion.inspectionTheme
import com.otoki.powersales.inspection.entity.QSiteActivity.Companion.siteActivity
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class InspectionThemeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : InspectionThemeRepositoryCustom {

    override fun findActiveThemesByDate(targetDate: LocalDate): List<InspectionTheme> {
        return queryFactory
            .selectFrom(inspectionTheme)
            .where(
                inspectionTheme.publicFlag.eq(true),
                inspectionTheme.startDate.loe(targetDate),
                inspectionTheme.endDate.goe(targetDate)
            )
            .orderBy(inspectionTheme.name.asc())
            .fetch()
    }

    override fun searchForAdmin(keyword: String?, pageable: Pageable): Page<InspectionTheme> {
        val where = BooleanBuilder()
        where.and(notDeleted())
        if (!keyword.isNullOrBlank()) {
            where.and(
                inspectionTheme.title.containsIgnoreCase(keyword)
                    .or(inspectionTheme.department.containsIgnoreCase(keyword))
                    .or(inspectionTheme.name.containsIgnoreCase(keyword))
            )
        }

        val content = queryFactory
            .selectFrom(inspectionTheme)
            .leftJoin(inspectionTheme.ownerUser).fetchJoin()
            .where(where)
            .orderBy(inspectionTheme.startDate.desc(), inspectionTheme.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(inspectionTheme.count())
            .from(inspectionTheme)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun countSiteActivitiesByThemeIds(themeIds: List<Long>): Map<Long, Long> {
        if (themeIds.isEmpty()) return emptyMap()
        return queryFactory
            .select(siteActivity.inspectionTheme.id, siteActivity.count())
            .from(siteActivity)
            .where(
                siteActivity.inspectionTheme.id.`in`(themeIds),
                siteActivity.isDeleted.isNull.or(siteActivity.isDeleted.isFalse)
            )
            .groupBy(siteActivity.inspectionTheme.id)
            .fetch()
            .associate { tuple ->
                tuple.get(siteActivity.inspectionTheme.id)!! to (tuple.get(siteActivity.count()) ?: 0L)
            }
    }

    override fun findMaxThemeNumberSequence(): Long {
        // 테마번호 형식 TM00000001 — 'TM' 이후 숫자 부분의 최대값.
        val maxName = queryFactory
            .select(inspectionTheme.name.max())
            .from(inspectionTheme)
            .where(inspectionTheme.name.startsWith(THEME_NUMBER_PREFIX))
            .fetchOne()
        return maxName
            ?.removePrefix(THEME_NUMBER_PREFIX)
            ?.toLongOrNull()
            ?: 0L
    }

    private fun notDeleted() =
        inspectionTheme.isDeleted.isNull.or(inspectionTheme.isDeleted.isFalse)

    companion object {
        const val THEME_NUMBER_PREFIX = "TM"
    }
}
