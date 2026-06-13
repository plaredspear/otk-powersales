package com.otoki.powersales.inspection.repository

import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityFilter
import com.otoki.powersales.inspection.entity.QInspectionTheme.Companion.inspectionTheme
import com.otoki.powersales.inspection.entity.QSiteActivity.Companion.siteActivity
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.product.entity.QProduct.Companion.product
import com.otoki.powersales.user.entity.QUser.Companion.user
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class SiteActivityRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : SiteActivityRepositoryCustom {

    override fun searchByEmployee(
        employeeId: Long,
        accountId: Long?,
        category: InspectionCategory?,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<SiteActivity> {
        val where = BooleanBuilder()
            .and(siteActivity.employee.id.eq(employeeId))
            .and(siteActivity.isDeleted.isNull.or(siteActivity.isDeleted.isFalse))
            .and(siteActivity.activityDate.goe(fromDate))
            .and(siteActivity.activityDate.loe(toDate))

        accountId?.let { where.and(siteActivity.account.id.eq(it)) }
        category?.let { where.and(siteActivity.productType.eq(it.storedValue)) }

        return queryFactory
            .selectFrom(siteActivity)
            .where(where)
            .orderBy(siteActivity.activityDate.desc(), siteActivity.id.desc())
            .fetch()
    }

    override fun searchForAdmin(
        policyPredicate: Predicate,
        filter: AdminSiteActivityFilter,
        pageable: Pageable
    ): Page<SiteActivity> {
        val where = BooleanBuilder()
            .and(policyPredicate)
            .and(siteActivity.isDeleted.isNull.or(siteActivity.isDeleted.isFalse))
            .and(siteActivity.activityDate.goe(filter.startDate))
            .and(siteActivity.activityDate.loe(filter.endDate))

        filter.category?.let { where.and(siteActivity.productType.eq(it.storedValue)) }
        filter.fieldType?.let { where.and(siteActivity.category.eq(it.displayName)) }
        filter.employeeName?.takeIf { it.isNotBlank() }
            ?.let { where.and(siteActivity.employee.name.containsIgnoreCase(it)) }
        filter.accountCode?.takeIf { it.isNotBlank() }
            ?.let { where.and(siteActivity.account.externalKey.eq(it)) }

        val content = queryFactory
            .selectFrom(siteActivity)
            .leftJoin(siteActivity.employee, employee).fetchJoin()
            .leftJoin(siteActivity.account, account).fetchJoin()
            .leftJoin(siteActivity.product, product).fetchJoin()
            .leftJoin(siteActivity.inspectionTheme, inspectionTheme).fetchJoin()
            // policyPredicate 의 owner/hierarchy 절이 ownerUser 를 참조하므로 명시 leftJoin 으로
            // 선언해 암묵 INNER JOIN 을 차단한다. 누락 시 owner_user_id NULL 행(모바일 등록 건)이
            // OR 의 다른 절(cost_center_code 등)로 통과해야 함에도 전부 누락된다.
            .leftJoin(siteActivity.ownerUser, user)
            .where(where)
            .orderBy(siteActivity.activityDate.desc(), siteActivity.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(siteActivity.count())
            .from(siteActivity)
            .leftJoin(siteActivity.employee, employee)
            .leftJoin(siteActivity.account, account)
            .leftJoin(siteActivity.ownerUser, user)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findByInspectionThemeIdForAdmin(themeId: Long): List<SiteActivity> {
        return queryFactory
            .selectFrom(siteActivity)
            .leftJoin(siteActivity.employee, employee).fetchJoin()
            .leftJoin(siteActivity.account, account).fetchJoin()
            .leftJoin(siteActivity.product, product).fetchJoin()
            .where(
                siteActivity.inspectionTheme.id.eq(themeId),
                siteActivity.isDeleted.isNull.or(siteActivity.isDeleted.isFalse)
            )
            .orderBy(siteActivity.activityDate.desc(), siteActivity.id.desc())
            .fetch()
    }

    override fun existsVisibleById(policyPredicate: Predicate, id: Long): Boolean {
        val where = BooleanBuilder()
            .and(policyPredicate)
            .and(siteActivity.id.eq(id))
            .and(siteActivity.isDeleted.isNull.or(siteActivity.isDeleted.isFalse))

        return queryFactory
            .selectOne()
            .from(siteActivity)
            .leftJoin(siteActivity.employee, employee)
            .leftJoin(siteActivity.account, account)
            .leftJoin(siteActivity.ownerUser, user)
            .where(where)
            .fetchFirst() != null
    }
}
