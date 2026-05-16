package com.otoki.powersales.organization.repository

import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.entity.QOrganization.Companion.organization
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory

class OrganizationRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrganizationRepositoryCustom {

    override fun findTeamScheduleBranches(hrCode: String?, allBranches: Boolean): List<BranchResponse> {
        val builder = BooleanBuilder()
        builder.and(organization.isDeleted.isNull.or(organization.isDeleted.isFalse))

        if (allBranches) {
            builder.and(
                organization.orgNameLevel3.eq("Retail사업부")
                    .or(organization.orgNameLevel3.eq("제1사업부"))
                    .or(organization.orgNameLevel4.eq("영업지원1팀"))
                    .or(organization.orgNameLevel4.eq("영업지원2팀"))
            )
        } else {
            if (hrCode.isNullOrBlank()) return emptyList()
            builder.and(
                organization.costCenterLevel5.eq(hrCode)
                    .or(organization.costCenterLevel4.eq(hrCode))
                    .or(organization.costCenterLevel3.eq(hrCode))
                    .or(organization.costCenterLevel2.eq(hrCode))
            )
            builder.and(
                organization.orgNameLevel3.`in`("Retail사업부", "제1사업부", "CVS사업부")
            )
        }

        return fetchTeamScheduleBranches(builder)
    }

    override fun findAllTeamScheduleBranches(): List<BranchResponse> {
        val builder = BooleanBuilder()
        builder.and(organization.isDeleted.isNull.or(organization.isDeleted.isFalse))
        return fetchTeamScheduleBranches(builder)
    }

    private fun fetchTeamScheduleBranches(where: BooleanBuilder): List<BranchResponse> {
        val tuples: List<Tuple> = queryFactory
            .select(
                organization.costCenterLevel5,
                organization.orgNameLevel5,
                organization.costCenterLevel4,
                organization.orgNameLevel4
            )
            .from(organization)
            .where(where)
            .orderBy(
                organization.orgCodeLevel3.asc(),
                organization.orgCodeLevel4.asc(),
                organization.orgCodeLevel5.asc()
            )
            .fetch()

        val seen = LinkedHashSet<String>()
        val result = mutableListOf<BranchResponse>()
        for (t in tuples) {
            val cc5 = t.get(0, String::class.java)
            val nm5 = t.get(1, String::class.java)
            val cc4 = t.get(2, String::class.java)
            val nm4 = t.get(3, String::class.java)

            val code = if (!cc5.isNullOrBlank()) cc5 else cc4
            val name = if (!nm5.isNullOrBlank()) nm5 else nm4
            if (code.isNullOrBlank() || name.isNullOrBlank()) continue
            if (seen.add(code)) {
                result += BranchResponse(code, name)
            }
        }
        return result
    }

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

    override fun findFirstByAnyOrgCodeLevel(orgCode: String): Organization? {
        return queryFactory
            .selectFrom(organization)
            .where(
                organization.orgCodeLevel2.eq(orgCode)
                    .or(organization.orgCodeLevel3.eq(orgCode))
                    .or(organization.orgCodeLevel4.eq(orgCode))
                    .or(organization.orgCodeLevel5.eq(orgCode))
            )
            .limit(1)
            .fetchFirst()
    }

    /**
     * cost_center 컬럼 cascade lookup — Level5 → Level4.
     *
     * 단일 JPQL 로 통합하지 않고 Level5 → Level4 derived 쿼리를 순차 호출하는 이유:
     *  - 첫 번째 hit 시 두 번째 쿼리 short-circuit (?: 단락 평가) — 다수 케이스에서 SELECT 1회로 종료
     *  - 단일 JPQL 통합 시 CASE-ORDER BY 가 필요해져 plan 복잡도 증가 + 가독성 하락
     *
     * 정책 변경(예: Level3 추가) 시 본 메서드 본문만 수정.
     */
    override fun findFirstByCostCenterCascade(costCenterCode: String): Organization? {
        return queryFactory
            .selectFrom(organization)
            .where(organization.costCenterLevel5.eq(costCenterCode))
            .limit(1)
            .fetchFirst()
            ?: queryFactory
                .selectFrom(organization)
                .where(organization.costCenterLevel4.eq(costCenterCode))
                .limit(1)
                .fetchFirst()
    }

    /**
     * org_code 컬럼 cascade lookup — Level5 → Level4 → Level3.
     *
     * 호출자: `EmployeeProfileResolver`, `UserRoleResolver` (SF AppointmentTriggerHanlder cascade 정합).
     * cascade 순서 변경 시 본 메서드 본문만 수정.
     */
    override fun findFirstByOrgCodeCascade(orgCode: String): Organization? {
        return queryFactory
            .selectFrom(organization)
            .where(organization.orgCodeLevel5.eq(orgCode))
            .limit(1)
            .fetchFirst()
            ?: queryFactory
                .selectFrom(organization)
                .where(organization.orgCodeLevel4.eq(orgCode))
                .limit(1)
                .fetchFirst()
            ?: queryFactory
                .selectFrom(organization)
                .where(organization.orgCodeLevel3.eq(orgCode))
                .limit(1)
                .fetchFirst()
    }

    override fun expandCostCenterCodes(costCenterCodes: List<String>): List<String> {
        if (costCenterCodes.isEmpty()) return emptyList()

        val builder = BooleanBuilder()
        builder.or(organization.costCenterLevel2.`in`(costCenterCodes))
        builder.or(organization.costCenterLevel3.`in`(costCenterCodes))
        builder.or(organization.costCenterLevel4.`in`(costCenterCodes))
        builder.or(organization.costCenterLevel5.`in`(costCenterCodes))

        return queryFactory
            .select(organization.costCenterLevel5).distinct()
            .from(organization)
            .where(builder, organization.costCenterLevel5.isNotNull)
            .fetch()
            .filterNotNull()
    }
}
