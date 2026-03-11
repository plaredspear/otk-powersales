package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.sap.entity.QOrganization.organization
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory

class OrganizationRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrganizationRepositoryCustom {

    override fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Organization> {
        val builder = BooleanBuilder()

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                organization.costCenterLevel2.lower().like(lowerPattern)
                    .or(organization.orgCodeLevel2.lower().like(lowerPattern))
                    .or(organization.orgNameLevel2.lower().like(lowerPattern))
                    .or(organization.costCenterLevel3.lower().like(lowerPattern))
                    .or(organization.orgCodeLevel3.lower().like(lowerPattern))
                    .or(organization.orgNameLevel3.lower().like(lowerPattern))
                    .or(organization.costCenterLevel4.lower().like(lowerPattern))
                    .or(organization.orgCodeLevel4.lower().like(lowerPattern))
                    .or(organization.orgNameLevel4.lower().like(lowerPattern))
                    .or(organization.costCenterLevel5.lower().like(lowerPattern))
                    .or(organization.orgCodeLevel5.lower().like(lowerPattern))
                    .or(organization.orgNameLevel5.lower().like(lowerPattern))
            )
        }

        if (!level.isNullOrBlank()) {
            when (level) {
                "L2" -> builder.and(organization.orgNameLevel2.isNotNull)
                "L3" -> builder.and(organization.orgNameLevel3.isNotNull)
                "L4" -> builder.and(organization.orgNameLevel4.isNotNull)
                "L5" -> builder.and(organization.orgNameLevel5.isNotNull)
            }
        }

        if (branchCodes != null) {
            builder.and(
                organization.costCenterLevel2.`in`(branchCodes)
                    .or(organization.costCenterLevel3.`in`(branchCodes))
                    .or(organization.costCenterLevel4.`in`(branchCodes))
                    .or(organization.costCenterLevel5.`in`(branchCodes))
            )
        }

        return queryFactory
            .selectFrom(organization)
            .where(builder)
            .orderBy(
                organization.costCenterLevel2.asc(),
                organization.costCenterLevel3.asc(),
                organization.costCenterLevel4.asc(),
                organization.costCenterLevel5.asc()
            )
            .fetch()
    }
}
