package com.otoki.powersales.domain.activity.inspection.repository

import com.otoki.powersales.domain.activity.inspection.entity.InspectionTheme
import com.otoki.powersales.domain.activity.inspection.entity.QInspectionTheme.Companion.inspectionTheme
import com.otoki.powersales.domain.activity.inspection.entity.QSiteActivity.Companion.siteActivity
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class InspectionThemeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : InspectionThemeRepositoryCustom {

    override fun findActiveThemesByDate(targetDate: LocalDate, costCenterCode: String?): List<InspectionTheme> {
        // 레거시(fieldTalkMapper.selectTheme) 정합:
        // - 오늘이 기간(startDate~endDate) 내인 테마만
        // - branch_code IN (공통 화이트리스트 + 사원 코스트센터)  ← 지점 스코프 제한
        // - publicFlag 는 필터 아님(레거시는 publicflag__c 를 조회만 하고 WHERE 에 미사용)
        // - 정렬은 생성일(createddate) 오름차순
        val branchCodes = COMMON_BRANCH_CODES + listOfNotNull(costCenterCode)
        return queryFactory
            .selectFrom(inspectionTheme)
            .where(
                inspectionTheme.startDate.loe(targetDate),
                inspectionTheme.endDate.goe(targetDate),
                inspectionTheme.branchCode.`in`(branchCodes)
            )
            .orderBy(inspectionTheme.createdAt.asc())
            .fetch()
    }

    override fun searchForAdmin(
        keyword: String?,
        department: String?,
        branchCode: String?,
        pageable: Pageable,
    ): Page<InspectionTheme> {
        val where = BooleanBuilder()
        where.and(notDeleted())
        if (!keyword.isNullOrBlank()) {
            where.and(
                inspectionTheme.title.containsIgnoreCase(keyword)
                    .or(inspectionTheme.department.containsIgnoreCase(keyword))
                    .or(inspectionTheme.name.containsIgnoreCase(keyword))
            )
        }
        // 부서 부분일치 / 지점코드 정확일치 — trigger 자동주입 값 기준 필터.
        if (!department.isNullOrBlank()) {
            where.and(inspectionTheme.department.containsIgnoreCase(department))
        }
        if (!branchCode.isNullOrBlank()) {
            where.and(inspectionTheme.branchCode.eq(branchCode))
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

        /**
         * 레거시 fieldTalkMapper.selectTheme 의 branchcode__c IN 하드코딩 화이트리스트.
         * 전사/공통 테마를 모든 사원이 볼 수 있게 하는 지점(코스트센터) 코드 목록.
         * 의미를 추적할 마스터가 레거시 소스에 없는 잔재이나, 신규 branch_code 가 동일 SF
         * Theme__c.BranchCode__c 에서 마이그레이션되므로 값은 그대로 유효하다.
         */
        private val COMMON_BRANCH_CODES = listOf(
            "3473", "4888", "4889", "5642", "5643", "5644", "5645", "5646", "5647",
            "5648", "5649", "5074", "5351", "5066", "1837", "3227", "3228", "3229",
            "3230", "3231", "3232", "3233", "3234", "3235", "3236", "3472"
        )
    }
}
