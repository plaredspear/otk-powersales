package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.entity.QPromotion.Companion.promotion
import com.otoki.powersales.domain.activity.promotion.entity.QPromotionEmployee.Companion.promotionEmployee
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.otoki.powersales.domain.activity.schedule.entity.QAttendanceLog.Companion.attendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.QTeamMemberSchedule.Companion.teamMemberSchedule
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.math.BigDecimal
import java.time.LocalDate

class PromotionEmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : PromotionEmployeeRepositoryCustom {

    // 행사사원 soft-delete(IsDeleted) 제외 — SF 정합 (모든 조회에서 IsDeleted=true row 자동 제외).
    // is_deleted 는 nullable(SF migration row 정합)이라 NULL 도 미삭제로 통과시킨다.
    private val notDeleted: Predicate =
        promotionEmployee.isDeleted.isNull.or(promotionEmployee.isDeleted.isFalse)

    override fun findAllAccessibleByParentPolicy(parentPolicyPredicate: Predicate): List<PromotionEmployee> {
        // ControlledByParent — 자식 PromotionEmployee 의 가시성은 부모 Promotion 의 정책 결과 흡수.
        // Service layer 가 SharingRulePolicyEvaluator.buildPredicate(scope, "DKRetail__Promotion__c", QPromotion) 호출.
        return queryFactory
            .selectFrom(promotionEmployee)
            .join(promotionEmployee.promotion, promotion)
            // parentPolicyPredicate 의 owner/hierarchy path (promotion.ownerUser.*) 가 implicit
            // inner join 을 만들지 않도록 명시적 leftJoin. OR 합성이라 부모 ownerUser=null row 도
            // 다른 절로 통과해야 한다 (PromotionRepositoryCustomImpl 동일 패턴).
            .leftJoin(promotion.ownerUser)
            .where(parentPolicyPredicate, notDeleted)
            .fetch()
    }

    override fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .leftJoin(promotionEmployee.employee, employee).fetchJoin()
            // 전문행사조(투입당시) 값 = teamMemberSchedule.professionalPromotionTeam (SF ScheduleId__r.ProfessionalPromotionTeam__c 동등)
            .leftJoin(promotionEmployee.teamMemberSchedule, teamMemberSchedule).fetchJoin()
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .orderBy(promotionEmployee.scheduleDate.asc())
            .fetch()
    }

    override fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.min())
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .fetchOne()
    }

    override fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.max())
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .fetchOne()
    }

    // 조원 목표금액 파생식 SUM = SUM(COALESCE(daily_target_count,0) * COALESCE(base_price,0)).
    private val dailyTargetAmountSum: NumberExpression<BigDecimal> =
        promotionEmployee.dailyTargetCount.coalesce(BigDecimal.ZERO)
            .multiply(promotionEmployee.basePrice.coalesce(BigDecimal.ZERO))
            .sumAggregate()

    // 조원 실적금액(총 실적) 파생식 SUM = SUM(COALESCE(primary_product_amount,0) + COALESCE(other_sales_amount,0)).
    private val dailyActualAmountSum: NumberExpression<BigDecimal> =
        promotionEmployee.primaryProductAmount.coalesce(BigDecimal.ZERO)
            .add(promotionEmployee.otherSalesAmount.coalesce(BigDecimal.ZERO))
            .sumAggregate()

    override fun sumTargetActualAmountByPromotionIds(promotionIds: Collection<Long>): Map<Long, Pair<Long, Long>> {
        if (promotionIds.isEmpty()) return emptyMap()
        return queryFactory
            .select(
                promotionEmployee.promotionId,
                dailyTargetAmountSum,
                dailyActualAmountSum,
            )
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.`in`(promotionIds), notDeleted)
            .groupBy(promotionEmployee.promotionId)
            .fetch()
            .associate { tuple ->
                val pid = tuple.get(promotionEmployee.promotionId)!!
                val target = tuple.get(dailyTargetAmountSum)?.toLong() ?: 0L
                val actual = tuple.get(dailyActualAmountSum)?.toLong() ?: 0L
                pid to (target to actual)
            }
    }

    override fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.min())
            .from(promotionEmployee)
            .where(
                promotionEmployee.promotionId.eq(promotionId),
                promotionEmployee.employeeId.eq(employeeId),
                notDeleted
            )
            .fetchOne()
    }

    /**
     * 목표/실적 보고서 공통 filter — 상세 조회와 소계 집계가 같은 모집단을 보도록 한 곳에서 만든다.
     *
     * 지점 스코프는 행사사원(사원 마스터) 소속 지점(employee.costCenterCode) IN. 빈 목록이면 전사(null → 미적용).
     * 여사원일정(teamMemberSchedule.costCenterCode)을 판정 축으로 쓰지 않는다: 그 컬럼은 출근 등록 시점에만
     * stamp 되고(AttendanceService.stampLegacyWorkReportMeta) 행사 확정으로 생성된 일정은 NULL 이라,
     * 지점을 고르는 순간 "출근한 행"만 남아 미출근/미확정 투입 계획이 통째로 누락됐다.
     * SF 리포트는 조원일정을 outer join(ProMasCollect.reportType outerJoin=true) 으로 붙여 일정/출근 유무와
     * 무관하게 행사사원 행을 모두 내보내므로, 지점 필터에서도 그 정합을 유지한다.
     */
    private fun targetActualReportPredicates(
        startDate: LocalDate,
        endDate: LocalDate,
        branchScopeCodes: List<String>,
    ): Array<Predicate?> = arrayOf(
        promotionEmployee.scheduleDate.between(startDate, endDate),
        notDeleted, // soft-delete 제외
        branchScopeCodes.takeIf { it.isNotEmpty() }?.let { employee.costCenterCode.`in`(it) },
    )

    override fun findTargetActualReport(
        startDate: LocalDate,
        endDate: LocalDate,
        branchScopeCodes: List<String>,
        limit: Int?,
    ): List<PromotionTargetActualReportRecord> {
        // DTO projection — 기간 조회 행 수가 수만 건 규모라 entity fetchJoin(6 entity 전 컬럼) 대신 사용 컬럼만 select.
        // attendanceLog 는 TeamMemberSchedule 파생 프로퍼티(isWorkReport/commuteDate)의 원천 — 조인에 포함해 N+1 회피.
        val query = queryFactory
            .select(
                Projections.constructor(
                    PromotionTargetActualReportRecord::class.java,
                    promotion.promotionNumber,
                    account.branchName,
                    account.name,
                    account.externalKey,
                    product.name,
                    product.storeConditionText,
                    promotion.otherProduct,
                    promotion.standLocation,
                    employee.employeeCode,
                    employee.orgName,
                    employee.name,
                    employee.professionalPromotionTeam,
                    teamMemberSchedule.professionalPromotionTeam,
                    promotionEmployee.scheduleDate,
                    promotionEmployee.dailyTargetCount,
                    promotionEmployee.basePrice,
                    promotionEmployee.primarySalesQuantity,
                    promotionEmployee.primaryProductAmount,
                    promotionEmployee.otherSalesQuantity,
                    promotionEmployee.otherSalesAmount,
                    promotionEmployee.dkWorkType2,
                    promotionEmployee.workType3,
                    attendanceLog.id,
                    attendanceLog.attendanceDate,
                ),
            )
            .from(promotionEmployee)
            .join(promotionEmployee.promotion, promotion)
            .leftJoin(promotion.account, account)
            .leftJoin(promotion.primaryProduct, product)
            .leftJoin(promotionEmployee.employee, employee)
            // isWorkReport / commuteDate 원천 컬럼은 TeamMemberSchedule→AttendanceLog 소유
            .leftJoin(promotionEmployee.teamMemberSchedule, teamMemberSchedule)
            .leftJoin(teamMemberSchedule.attendanceLog, attendanceLog)
            .where(*targetActualReportPredicates(startDate, endDate, branchScopeCodes))
            // Summary 그룹 재현 — 행사명(promotion.Name = promotionNumber) 그룹 + 그룹 내 일자 오름차순
            .orderBy(promotion.promotionNumber.asc(), promotionEmployee.scheduleDate.asc())

        // 출력에 쓰지 않을 행은 애초에 가져오지 않는다 (화면 상한). limit 이 null 이면 조건 전량 = 엑셀 export.
        if (limit != null) query.limit(limit.toLong())
        return query.fetch()
    }

    override fun findTargetActualReportSubtotals(
        startDate: LocalDate,
        endDate: LocalDate,
        branchScopeCodes: List<String>,
    ): List<PromotionTargetActualReportSubtotal> {
        // 소계/합계/차트는 상세 행 상한과 무관하게 전량 기준이어야 하므로 DB 집계로 분리한다.
        // 조인은 그룹 키(promotion) + 지점 스코프(employee) 만 — 나머지 관계는 단일값 @ManyToOne 이라
        // 생략해도 행 수가 동일해 집계값이 상세 조회와 일치한다.
        return queryFactory
            .select(
                Projections.constructor(
                    PromotionTargetActualReportSubtotal::class.java,
                    promotion.promotionNumber,
                    dailyTargetAmountSum,
                    dailyActualAmountSum,
                    promotionEmployee.primarySalesQuantity.coalesce(BigDecimal.ZERO).sumAggregate(),
                    promotionEmployee.primaryProductAmount.coalesce(BigDecimal.ZERO).sumAggregate(),
                    promotionEmployee.otherSalesQuantity.coalesce(BigDecimal.ZERO).sumAggregate(),
                    promotionEmployee.otherSalesAmount.coalesce(BigDecimal.ZERO).sumAggregate(),
                    promotionEmployee.count(),
                ),
            )
            .from(promotionEmployee)
            .join(promotionEmployee.promotion, promotion)
            .leftJoin(promotionEmployee.employee, employee)
            .where(*targetActualReportPredicates(startDate, endDate, branchScopeCodes))
            .groupBy(promotion.promotionNumber)
            // 상세 조회와 같은 그룹 순서 (Summary 그룹 재현)
            .orderBy(promotion.promotionNumber.asc())
            .fetch()
    }

    override fun findMyAssignmentsByDate(employeeId: Long, date: LocalDate): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .join(promotionEmployee.promotion, promotion).fetchJoin()
            .leftJoin(promotion.account, account).fetchJoin()
            .where(
                promotionEmployee.employeeId.eq(employeeId),
                promotionEmployee.scheduleDate.eq(date),
                notDeleted,
                promotion.isDeleted.isFalse,
            )
            .orderBy(promotion.promotionNumber.asc())
            .fetch()
    }

    override fun findConfirmedAssignedAccountIdsByEmployeeAndDate(employeeId: Long, date: LocalDate): List<Long> {
        return queryFactory
            .select(promotion.account.id).distinct()
            .from(promotionEmployee)
            .join(promotionEmployee.promotion, promotion)
            .where(
                promotionEmployee.employeeId.eq(employeeId),
                // 확정 여부 — 행사 확정(PromotionSchedulesUpsertHelper.upsert)이 채우는 일정 백링크.
                promotionEmployee.teamMemberScheduleId.isNotNull,
                // 행사마스터 기간이 date 를 포함 (여사원 개인 투입일 scheduleDate 축이 아님).
                promotion.startDate.loe(date),
                promotion.endDate.goe(date),
                notDeleted,
                promotion.isDeleted.isFalse,
                promotion.account.id.isNotNull,
            )
            .fetch()
            .filterNotNull()
    }

    override fun findByPromotionId(promotionId: Long): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .fetch()
    }

    override fun existsByPromotionIdAndPromoCloseByTmTrue(promotionId: Long): Boolean {
        return queryFactory
            .selectOne()
            .from(promotionEmployee)
            .where(
                promotionEmployee.promotionId.eq(promotionId),
                promotionEmployee.promoCloseByTm.isTrue,
                notDeleted,
            )
            .fetchFirst() != null
    }

    override fun existsByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): Boolean {
        return queryFactory
            .selectOne()
            .from(promotionEmployee)
            .where(
                promotionEmployee.promotionId.eq(promotionId),
                promotionEmployee.employeeId.eq(employeeId),
                notDeleted,
            )
            .fetchFirst() != null
    }
}
