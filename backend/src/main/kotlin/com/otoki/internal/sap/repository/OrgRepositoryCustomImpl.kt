package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.entity.QOrg.org
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory

class OrgRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrgRepositoryCustom {

    override fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Org> {
        val builder = BooleanBuilder()

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                org.costCenterLevel2.lower().like(lowerPattern)
                    .or(org.orgCodeLevel2.lower().like(lowerPattern))
                    .or(org.orgNameLevel2.lower().like(lowerPattern))
                    .or(org.costCenterLevel3.lower().like(lowerPattern))
                    .or(org.orgCodeLevel3.lower().like(lowerPattern))
                    .or(org.orgNameLevel3.lower().like(lowerPattern))
                    .or(org.costCenterLevel4.lower().like(lowerPattern))
                    .or(org.orgCodeLevel4.lower().like(lowerPattern))
                    .or(org.orgNameLevel4.lower().like(lowerPattern))
                    .or(org.costCenterLevel5.lower().like(lowerPattern))
                    .or(org.orgCodeLevel5.lower().like(lowerPattern))
                    .or(org.orgNameLevel5.lower().like(lowerPattern))
            )
        }

        if (!level.isNullOrBlank()) {
            when (level) {
                "L2" -> builder.and(org.orgNameLevel2.isNotNull)
                "L3" -> builder.and(org.orgNameLevel3.isNotNull)
                "L4" -> builder.and(org.orgNameLevel4.isNotNull)
                "L5" -> builder.and(org.orgNameLevel5.isNotNull)
            }
        }

        if (branchCodes != null) {
            builder.and(
                org.costCenterLevel2.`in`(branchCodes)
                    .or(org.costCenterLevel3.`in`(branchCodes))
                    .or(org.costCenterLevel4.`in`(branchCodes))
                    .or(org.costCenterLevel5.`in`(branchCodes))
            )
        }

        return queryFactory
            .selectFrom(org)
            .where(builder)
            .orderBy(
                org.costCenterLevel2.asc(),
                org.costCenterLevel3.asc(),
                org.costCenterLevel4.asc(),
                org.costCenterLevel5.asc()
            )
            .fetch()
    }
}
