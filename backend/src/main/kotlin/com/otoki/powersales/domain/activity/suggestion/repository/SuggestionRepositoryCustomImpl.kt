package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.otoki.powersales.domain.activity.suggestion.entity.QSuggestion.Companion.suggestion
import com.otoki.powersales.user.entity.QUser.Companion.user
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 제안 Querydsl Impl (Spec #830 P1-B §2.4).
 */
class SuggestionRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : SuggestionRepositoryCustom {

    override fun searchForAdmin(
        policyPredicate: Predicate,
        filter: AdminSuggestionFilter,
        pageable: Pageable
    ): Page<Suggestion> {
        val where = BooleanBuilder()
            .and(policyPredicate)
            .and(suggestion.isDeleted.eq(false))
            .and(suggestion.createdAt.between(filter.startDateTime, filter.endDateTime))
            .and(categoryEq(filter))
            .and(employeeNameLike(filter))
            .and(accountCodeEq(filter))
            .and(actionStatusEq(filter))
            .and(productCodeEq(filter))

        val content = queryFactory
            .selectFrom(suggestion)
            .leftJoin(suggestion.employee, employee).fetchJoin()
            .leftJoin(suggestion.account, account).fetchJoin()
            .leftJoin(suggestion.product, product).fetchJoin()
            // policyPredicate 의 owner/hierarchy 절이 ownerUser 를 참조하므로 명시 leftJoin 으로
            // 선언해 암묵 INNER JOIN 을 차단한다. 누락 시 owner_user_id NULL 행이 OR 의 다른
            // 절(cost_center_code 등)로 통과해야 함에도 전부 누락된다.
            .leftJoin(suggestion.ownerUser, user)
            .where(where)
            .orderBy(suggestion.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(suggestion.count())
            .from(suggestion)
            .leftJoin(suggestion.employee, employee)
            .leftJoin(suggestion.account, account)
            .leftJoin(suggestion.product, product)
            .leftJoin(suggestion.ownerUser, user)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean {
        val where = BooleanBuilder()
            .and(suggestion.id.eq(id))
            .and(suggestion.isDeleted.eq(false))
            .and(policyPredicate)

        return queryFactory
            .selectOne()
            .from(suggestion)
            .leftJoin(suggestion.ownerUser, user)
            .where(where)
            .fetchFirst() != null
    }

    override fun searchMine(
        employeeId: Long,
        scopeOrgCostCenterCode: String?,
        category: SuggestionCategory?,
        accountId: Long?,
        createdFrom: LocalDateTime?,
        createdToExclusive: LocalDateTime?,
        pageable: Pageable
    ): Page<Suggestion> {
        val where = BooleanBuilder()
            .and(suggestion.isDeleted.eq(false))
            // 스코프: 원가센터 전체(조장·지점장 물류클레임) 또는 본인분(여사원/정규 제안).
            .and(
                if (scopeOrgCostCenterCode != null) suggestion.orgCostCenterCode.eq(scopeOrgCostCenterCode)
                else suggestion.employee.id.eq(employeeId)
            )
        category?.let { where.and(suggestion.category.eq(it)) }
        accountId?.let { where.and(suggestion.account.id.eq(it)) }
        // 레거시 LogisticsClaimSearch: 기간 필터는 CreatedDate 기준(발생일자 아님), 종료일 익일 미만 경계.
        createdFrom?.let { where.and(suggestion.createdAt.goe(it)) }
        createdToExclusive?.let { where.and(suggestion.createdAt.lt(it)) }

        val content = queryFactory
            .selectFrom(suggestion)
            .where(where)
            .orderBy(suggestion.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(suggestion.count())
            .from(suggestion)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findLogisticsClaimReport(
        startDate: LocalDate,
        endDate: LocalDate,
        branchScopeCodes: List<String>,
    ): List<Suggestion> {
        return queryFactory
            .selectFrom(suggestion)
            .leftJoin(suggestion.employee, employee).fetchJoin()
            .leftJoin(suggestion.account, account).fetchJoin()
            .leftJoin(suggestion.product, product).fetchJoin()
            // CUST_NAME 컬럼 — SF Report 의사 컬럼 = 레코드 Owner. ownerUser 조인.
            .leftJoin(suggestion.ownerUser, user).fetchJoin()
            .where(
                // 물류 클레임 분류만 (SF Category__c = '물류 클레임')
                suggestion.category.eq(SuggestionCategory.LOGISTICS_CLAIM),
                suggestion.claimDate.between(startDate, endDate),
                suggestion.isDeleted.eq(false),
                // SF WERK1_TX/WERK3_TX 'contains 빈값' 은 no-op (항상 참) — 미구현
                // 지점 스코프 — 등록 사원 소속 지점(suggestion.orgCostCenterCode) IN. 빈 목록이면 전사(null → 미적용).
                branchScopeCodes.takeIf { it.isNotEmpty() }?.let { suggestion.orgCostCenterCode.`in`(it) },
            )
            .orderBy(suggestion.claimDate.desc())
            .fetch()
    }

    private fun categoryEq(filter: AdminSuggestionFilter): Predicate? =
        filter.category?.let { suggestion.category.eq(it) }

    private fun employeeNameLike(filter: AdminSuggestionFilter): Predicate? {
        val name = filter.employeeName?.trim().orEmpty()
        if (name.isBlank()) return null
        return employee.name.like("%$name%")
    }

    private fun accountCodeEq(filter: AdminSuggestionFilter): Predicate? {
        val code = filter.accountCode?.trim().orEmpty()
        if (code.isBlank()) return null
        return account.externalKey.eq(code)
    }

    private fun actionStatusEq(filter: AdminSuggestionFilter): Predicate? =
        filter.actionStatus?.let { suggestion.actionStatus.eq(it) }

    private fun productCodeEq(filter: AdminSuggestionFilter): Predicate? {
        val code = filter.productCode?.trim().orEmpty()
        if (code.isBlank()) return null
        return product.productCode.eq(code)
    }
}
