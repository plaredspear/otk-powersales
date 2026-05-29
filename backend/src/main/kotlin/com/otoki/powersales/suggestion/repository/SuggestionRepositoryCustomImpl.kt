package com.otoki.powersales.suggestion.repository

import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.product.entity.QProduct.Companion.product
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.suggestion.entity.QSuggestion.Companion.suggestion
import com.otoki.powersales.suggestion.entity.Suggestion
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

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
            .where(where)
            .fetchFirst() != null
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
        return suggestion.productCode.eq(code)
    }
}
