package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.enums.PromotionType
import com.otoki.powersales.domain.activity.promotion.entity.QPromotion.Companion.promotion
import com.otoki.powersales.domain.activity.promotion.entity.QPromotionEmployee.Companion.promotionEmployee
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate

class PromotionRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PromotionRepositoryCustom {

    override fun findByIdWithRelations(id: Long): Promotion? {
        return queryFactory
            .selectFrom(promotion)
            .leftJoin(promotion.account, account).fetchJoin()
            .leftJoin(promotion.primaryProduct, product).fetchJoin()
            // 상세의 "작성자"(createdBy.name) LazyInit 회피 — LAZY @ManyToOne 을 fetchJoin 으로 한 번에 적재.
            .leftJoin(promotion.createdBy).fetchJoin()
            .where(promotion.id.eq(id))
            .fetchOne()
    }

    override fun searchForAdmin(
        policyPredicate: Predicate,
        keyword: String?,
        promotionType: PromotionType?,
        startDate: String?,
        endDate: String?,
        accountName: String?,
        accountNumber: String?,
        ownerOnly: Boolean,
        currentUserId: Long?,
        pageable: Pageable
    ): Page<Promotion> {
        val builder = BooleanBuilder()

        builder.and(promotion.isDeleted.eq(false))
        builder.and(policyPredicate)

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                promotion.promotionNumber.lower().like(lowerPattern)
            )
        }

        // 거래처 필터 — 진열스케줄마스터 정합. 거래처명/거래처코드(externalKey) OR like 검색.
        if (!accountName.isNullOrBlank()) {
            val lowerPattern = "%${accountName.lowercase()}%"
            builder.and(
                account.name.lower().like(lowerPattern)
                    .or(account.externalKey.lower().like(lowerPattern))
            )
        }

        // 거래처번호(accountNumber, SF AccountNumber) like 검색 — 거래처코드(externalKey)와 별개 필드.
        if (!accountNumber.isNullOrBlank()) {
            builder.and(
                account.accountNumber.lower().like("%${accountNumber.lowercase()}%")
            )
        }

        if (promotionType != null) {
            builder.and(promotion.promotionType.eq(promotionType))
        }

        if (!startDate.isNullOrBlank()) {
            val date = LocalDate.parse(startDate)
            builder.and(promotion.endDate.goe(date))
        }

        if (!endDate.isNullOrBlank()) {
            val date = LocalDate.parse(endDate)
            builder.and(promotion.startDate.loe(date))
        }

        // SF 웹 ListView filterScope=Mine 대응 — 가시 범위 안에서 내가 owner 인 행사만.
        // currentUserId 가 null 이면 의도적으로 매칭 0건 (소유자 불명 = 내 것 없음).
        if (ownerOnly) {
            builder.and(
                if (currentUserId != null) {
                    promotion.ownerUser.id.eq(currentUserId)
                } else {
                    Expressions.FALSE.isTrue
                }
            )
        }

        val content = queryFactory
            .selectFrom(promotion)
            .leftJoin(promotion.account, account).fetchJoin()
            .leftJoin(promotion.primaryProduct, product).fetchJoin()
            // 목록의 "작성자" 컬럼(createdBy.name) N+1 방지 — LAZY @ManyToOne 을 fetchJoin 으로 한 번에 적재.
            .leftJoin(promotion.createdBy).fetchJoin()
            // policyPredicate 의 owner/hierarchy path (promotion.ownerUser.*) 가 implicit inner join 을
            // 만들지 않도록 명시적 leftJoin. OR 합성이라 ownerUser=null row 도 다른 절로 통과해야 함.
            .leftJoin(promotion.ownerUser)
            .where(builder)
            .orderBy(promotion.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(promotion.count())
            .from(promotion)
            // 거래처 필터(account.name/externalKey/accountNumber) 가 builder 에 포함될 수 있으므로 count 에도 account join.
            .leftJoin(promotion.account, account)
            .leftJoin(promotion.ownerUser)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean {
        val where = BooleanBuilder()
            .and(promotion.id.eq(id))
            .and(promotion.isDeleted.eq(false))
            .and(policyPredicate)

        return queryFactory
            .selectOne()
            .from(promotion)
            .leftJoin(promotion.ownerUser)
            .where(where)
            .fetchFirst() != null
    }

    override fun searchForMobile(
        employeeId: Long?,
        costCenterCode: String?,
        isWoman: Boolean,
        keyword: String?,
        startDate: String?,
        endDate: String?,
        accountId: Long?,
        pageable: Pageable
    ): Page<Promotion> {
        val builder = BooleanBuilder()

        builder.and(promotion.isDeleted.eq(false))

        // 권한 기반 필터
        if (isWoman) {
            // 여사원: 본인이 배정된 행사만 (PromotionEmployee.employeeId = User PK)
            builder.and(
                promotion.id.`in`(
                    JPAExpressions
                        .select(promotionEmployee.promotionId)
                        .from(promotionEmployee)
                        .where(promotionEmployee.employeeId.eq(employeeId))
                )
            )
        } else {
            // 조장/팀장: 같은 지점 행사 전체
            builder.and(promotion.costCenterCode.eq(costCenterCode))
        }

        // 키워드 검색 (행사번호, 거래처명)
        val hasKeyword = !keyword.isNullOrBlank()
        if (hasKeyword) {
            val lowerPattern = "%${keyword!!.lowercase()}%"
            builder.and(
                promotion.promotionNumber.lower().like(lowerPattern)
                    .or(account.name.lower().like(lowerPattern))
            )
        }

        // 거래처 필터 (레거시 SAPAccountCode → account PK)
        if (accountId != null) {
            builder.and(promotion.account.id.eq(accountId))
        }

        // 날짜 필터 (기간 겹침)
        if (!startDate.isNullOrBlank()) {
            val date = LocalDate.parse(startDate)
            builder.and(promotion.endDate.goe(date))
        }

        if (!endDate.isNullOrBlank()) {
            val date = LocalDate.parse(endDate)
            builder.and(promotion.startDate.loe(date))
        }

        val content = queryFactory
            .selectFrom(promotion)
            .leftJoin(promotion.account, account).fetchJoin()
            .where(builder)
            .orderBy(promotion.startDate.desc(), promotion.promotionNumber.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(promotion.count())
            .from(promotion)
            .leftJoin(promotion.account, account)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
