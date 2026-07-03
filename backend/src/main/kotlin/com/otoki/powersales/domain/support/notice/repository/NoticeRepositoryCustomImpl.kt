package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.domain.support.notice.enums.NoticeStatus
import com.otoki.powersales.domain.support.notice.entity.QNotice.Companion.notice
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.org.employee.entity.QEmployeeInfo.Companion.employeeInfo
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
        branchCode: String,
        pageable: Pageable
    ): Page<Notice> {
        val where = BooleanBuilder()
            .and(buildDeletedCondition())
            .and(buildPublishedCondition())
            .and(buildScopeCondition())
            .and(buildCategoryCondition(category, branchCode))
            .and(buildSearchCondition(search))

        val content = queryFactory
            .selectFrom(notice)
            .leftJoin(notice.employee).fetchJoin()
            .leftJoin(notice.ownerUser).fetchJoin()
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
            .leftJoin(notice.employee).fetchJoin()
            .leftJoin(notice.ownerUser).fetchJoin()
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
        branchCode: String
    ): List<Notice> {
        return queryFactory
            .selectFrom(notice)
            .where(
                buildDeletedCondition(),
                buildPublishedCondition(),
                buildScopeCondition(),
                notice.category.eq(NoticeCategory.COMPANY)
                    .or(notice.category.eq(NoticeCategory.BRANCH).and(notice.branchCode.eq(branchCode)))
                    .or(notice.category.eq(NoticeCategory.EDUCATION))
            )
            .orderBy(notice.createdAt.desc())
            .limit(5)
            .fetch()
    }

    override fun findPushTargetTokens(
        category: NoticeCategory,
        branchCode: String?
    ): List<String> {
        // 지점공지는 지점코드 매칭 사용자만, 그 외(회사/교육)는 전 사용자.
        // 지점공지인데 branchCode 가 비면 대상 없음 (오발송 방지).
        val branchCondition = if (category == NoticeCategory.BRANCH) {
            if (branchCode.isNullOrBlank()) return emptyList()
            employee.costCenterCode.eq(branchCode)
        } else {
            null
        }

        return queryFactory
            .select(employeeInfo.fcmToken)
            .distinct()
            .from(employee)
            .join(employee.employeeInfo, employeeInfo)
            .where(
                employeeInfo.fcmToken.isNotNull,
                employeeInfo.fcmToken.ne(""),
                branchCondition,
            )
            .fetch()
    }

    private fun buildDeletedCondition(): Predicate {
        return notice.isDeleted.isNull.or(notice.isDeleted.eq(false))
    }

    /**
     * 발행 노출 조건 — 모바일/홈 사용자용 조회는 발행(PUBLISHED)된 공지만 노출한다.
     * 임시저장(DRAFT)은 관리자 전용. (admin 목록 findAllNotices 에는 적용하지 않아 draft 포함 조회.)
     */
    private fun buildPublishedCondition(): Predicate {
        return notice.status.eq(NoticeStatus.PUBLISHED)
    }

    /**
     * 공개범위(scope) 노출 조건 — 레거시 `communityMapper.xml#selectNotice` 의
     * `AND (dkretail__scope__c = '전체' OR dkretail__scope__c = '현장여사원')` 재현.
     *
     * 레거시는 scope 화이트리스트('전체' + '현장여사원')로 '영업사원' 공지를 홈/목록에서 제외한다
     * (예: 【판매전략실】 공지는 scope='영업사원'). 신규 SF 피크리스트 `DKRetail__Scope__c` 는
     * `영업사원`/`현장여사원` 2값만 존재하며 '전체' 는 레거시 UI 상수일 뿐 실데이터에 없으므로,
     * "영업사원 제외"(= scope IS NULL OR scope = 현장여사원) 로 변환한다.
     * null scope(미지정/구 '전체' 잔재)는 레거시 '전체' 의도에 맞춰 노출 유지.
     */
    private fun buildScopeCondition(): Predicate {
        return notice.scope.isNull.or(notice.scope.ne(NoticeScope.SALES_EMPLOYEE))
    }

    private fun buildAdminCategoryCondition(category: NoticeCategory?): Predicate? {
        return when (category) {
            null -> null
            else -> notice.category.eq(category)
        }
    }

    /**
     * 분류/지점 노출 조건 — 레거시 `communityMapper.xml#selectNotice` 재현.
     *
     * 레거시는 지점공지를 지점**코드**로 필터한다:
     *   `dkretail__category__c = '영업부/지점공지' AND dkretail__jeejumcode__c = #{branch}`
     * 이때 `#{branch}` 는 로그인 사용자의 `costcentercode__c` 다.
     *
     * 따라서 신규에서도 지점명(`notice.branch` = `DKRetail__Jeejum__c`)이 아니라
     * 지점코드(`notice.branchCode` = `DKRetail__JeejumCode__c`)를 사용자
     * `employee.costCenterCode`(= `CostCenterCode__c`)와 매칭한다. 지점명/조직명 포맷이
     * 달라 매칭 실패(지점공지 누락)하던 문제를 코드 기반 매칭으로 해소.
     */
    private fun buildCategoryCondition(category: NoticeCategory?, branchCode: String): Predicate {
        return when (category) {
            null -> notice.category.eq(NoticeCategory.COMPANY)
                .or(notice.category.eq(NoticeCategory.BRANCH).and(notice.branchCode.eq(branchCode)))
            else -> {
                if (category == NoticeCategory.BRANCH) {
                    notice.category.eq(NoticeCategory.BRANCH).and(notice.branchCode.eq(branchCode))
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
