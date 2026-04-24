package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.QPromotion.promotion
import com.otoki.powersales.promotion.entity.QPromotionEmployee.promotionEmployee
import com.otoki.powersales.promotion.entity.QPromotionType.promotionType
import com.otoki.powersales.sap.entity.QAccount.account
import com.otoki.powersales.sap.entity.QProduct.product
import com.querydsl.core.BooleanBuilder
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
            .leftJoin(promotion.promotionType, promotionType).fetchJoin()
            .leftJoin(promotion.account, account).fetchJoin()
            .leftJoin(promotion.primaryProduct, product).fetchJoin()
            .where(promotion.id.eq(id))
            .fetchOne()
    }

    override fun searchForAdmin(
        keyword: String?,
        promotionTypeId: Long?,
        category: String?,
        startDate: String?,
        endDate: String?,
        branchCodes: List<String>?,
        pageable: Pageable
    ): Page<Promotion> {
        val builder = BooleanBuilder()

        builder.and(promotion.isDeleted.eq(false))

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                promotion.promotionName.lower().like(lowerPattern)
                    .or(promotion.promotionNumber.lower().like(lowerPattern))
            )
        }

        if (promotionTypeId != null) {
            builder.and(promotion.promotionTypeId.eq(promotionTypeId))
        }

        if (!category.isNullOrBlank()) {
            builder.and(promotion.category.eq(category))
        }

        if (!startDate.isNullOrBlank()) {
            val date = LocalDate.parse(startDate)
            builder.and(promotion.endDate.goe(date))
        }

        if (!endDate.isNullOrBlank()) {
            val date = LocalDate.parse(endDate)
            builder.and(promotion.startDate.loe(date))
        }

        if (branchCodes != null) {
            builder.and(promotion.costCenterCode.`in`(branchCodes))
        }

        val content = queryFactory
            .selectFrom(promotion)
            .leftJoin(promotion.promotionType, promotionType).fetchJoin()
            .leftJoin(promotion.account, account).fetchJoin()
            .where(builder)
            .orderBy(promotion.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(promotion.count())
            .from(promotion)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun searchForMobile(
        employeeId: Long?,
        costCenterCode: String?,
        isWoman: Boolean,
        keyword: String?,
        startDate: String?,
        endDate: String?,
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

        // 키워드 검색 (행사명, 행사번호, 거래처명)
        val hasKeyword = !keyword.isNullOrBlank()
        if (hasKeyword) {
            val lowerPattern = "%${keyword!!.lowercase()}%"
            builder.and(
                promotion.promotionName.lower().like(lowerPattern)
                    .or(promotion.promotionNumber.lower().like(lowerPattern))
                    .or(account.name.lower().like(lowerPattern))
            )
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
