package com.otoki.powersales.domain.sales.dto.response

import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.service.SalesProgressRateMasterActualsResolver.Actuals
import java.time.LocalDateTime

/**
 * 거래처목표등록마스터 목록 행 (SF `SalesProgressRateMaster__c` ListView "모두" 15컬럼 동등).
 *
 * SF Formula 필드는 DB 컬럼이 없어 응답 산출/조인으로 대체:
 * - accountName/accountBranchName/accountCode/accountType — Account lookup 조인
 * - targetSum — RT+FR+RM+FO 합산 (SF `TargetSum__c`)
 * - progressRate — currentMonthSalesAmount / targetSum 비율 (SF `ProgressRate__c`, Percent). targetSum=0 시 null.
 *
 * 당월/전월 실적은 저장 컬럼이 아니라 조회 시점에 월매출이력에서 산출한 값이다
 * ([com.otoki.powersales.domain.sales.service.SalesProgressRateMasterActualsResolver]).
 * `*SourceUpdatedAt` 은 그 원천 row 의 최종 적재 시각으로, 화면이 "언제 갱신된 숫자인지" 를 표기한다
 * (월매출이력 row 가 없어 이관 스냅샷으로 폴백한 경우 null).
 */
data class SalesProgressRateMasterListItem(
    val id: Long,
    val name: String?,
    val targetYear: String?,
    val targetMonth: String?,
    val accountName: String?,
    val accountBranchName: String?,
    val accountCode: String?,
    val accountType: String?,
    val rtTargetAmount: Double?,
    val rmTargetAmount: Double?,
    val frTargetAmount: Double?,
    val foTargetAmount: Double?,
    val targetSum: Double,
    val currentMonthSalesAmount: Double?,
    val previousMonthSalesAmount: Double?,
    val progressRate: Double?,
    val currentMonthSourceUpdatedAt: LocalDateTime?,
    val previousMonthSourceUpdatedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: SalesProgressRateMaster, actuals: Actuals): SalesProgressRateMasterListItem {
            val targetSum = sumTarget(entity)
            return SalesProgressRateMasterListItem(
                id = entity.id,
                name = entity.name,
                targetYear = entity.targetYear,
                targetMonth = entity.targetMonth,
                accountName = entity.account?.name,
                accountBranchName = entity.account?.branchName,
                accountCode = entity.account?.externalKey,
                accountType = entity.account?.accountType,
                rtTargetAmount = entity.rtTargetAmount,
                rmTargetAmount = entity.rmTargetAmount,
                frTargetAmount = entity.frTargetAmount,
                foTargetAmount = entity.foTargetAmount,
                targetSum = targetSum,
                currentMonthSalesAmount = actuals.currentMonthSalesAmount,
                previousMonthSalesAmount = actuals.previousMonthSalesAmount,
                progressRate = progressRate(actuals.currentMonthSalesAmount, targetSum),
                currentMonthSourceUpdatedAt = actuals.currentMonthSourceUpdatedAt,
                previousMonthSourceUpdatedAt = actuals.previousMonthSourceUpdatedAt,
            )
        }
    }
}

data class SalesProgressRateMasterListResponse(
    val content: List<SalesProgressRateMasterListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/**
 * 거래처목표등록마스터 상세 — 목록 필드 + 영업일 진도율/외부키/감사 정보.
 *
 * 당월/전월 실적 + `*SourceUpdatedAt` 의 의미는 [SalesProgressRateMasterListItem] 과 동일하다
 * (조회 시점 월매출이력 산출값 + 원천 적재 시각).
 */
data class SalesProgressRateMasterDetailResponse(
    val id: Long,
    val name: String?,
    val targetYear: String?,
    val targetMonth: String?,
    val accountId: Long?,
    val accountName: String?,
    val accountBranchName: String?,
    val accountCode: String?,
    val accountType: String?,
    val rtTargetAmount: Double?,
    val rmTargetAmount: Double?,
    val frTargetAmount: Double?,
    val foTargetAmount: Double?,
    val targetSum: Double,
    val targetSumAmount: Double?,
    val currentMonthSalesAmount: Double?,
    val previousMonthSalesAmount: Double?,
    val progressRate: Double?,
    val currentMonthSourceUpdatedAt: LocalDateTime?,
    val previousMonthSourceUpdatedAt: LocalDateTime?,
    val businessRate: Double?,
    val externalKey: String?,
    val accountBranchView: String?,
    val createdByName: String?,
    val lastModifiedByName: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: SalesProgressRateMaster, actuals: Actuals): SalesProgressRateMasterDetailResponse {
            val targetSum = sumTarget(entity)
            return SalesProgressRateMasterDetailResponse(
                id = entity.id,
                name = entity.name,
                targetYear = entity.targetYear,
                targetMonth = entity.targetMonth,
                accountId = entity.account?.id,
                accountName = entity.account?.name,
                accountBranchName = entity.account?.branchName,
                accountCode = entity.account?.externalKey,
                accountType = entity.account?.accountType,
                rtTargetAmount = entity.rtTargetAmount,
                rmTargetAmount = entity.rmTargetAmount,
                frTargetAmount = entity.frTargetAmount,
                foTargetAmount = entity.foTargetAmount,
                targetSum = targetSum,
                targetSumAmount = entity.targetSumAmount,
                currentMonthSalesAmount = actuals.currentMonthSalesAmount,
                previousMonthSalesAmount = actuals.previousMonthSalesAmount,
                progressRate = progressRate(actuals.currentMonthSalesAmount, targetSum),
                currentMonthSourceUpdatedAt = actuals.currentMonthSourceUpdatedAt,
                previousMonthSourceUpdatedAt = actuals.previousMonthSourceUpdatedAt,
                businessRate = entity.businessRate,
                externalKey = entity.externalKey,
                accountBranchView = entity.accountBranchView,
                createdByName = entity.createdBy?.name,
                lastModifiedByName = entity.lastModifiedBy?.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }
}

/** RT+FR+RM+FO 합산 (null 채널은 0 처리). SF `TargetSum__c` 수식 동등. */
private fun sumTarget(entity: SalesProgressRateMaster): Double =
    (entity.rtTargetAmount ?: 0.0) +
        (entity.frTargetAmount ?: 0.0) +
        (entity.rmTargetAmount ?: 0.0) +
        (entity.foTargetAmount ?: 0.0)

/** currentMonthSalesAmount / targetSum 비율. targetSum<=0 이면 null. SF `ProgressRate__c` 수식 동등. */
private fun progressRate(currentMonthSalesAmount: Double?, targetSum: Double): Double? =
    if (targetSum > 0.0) (currentMonthSalesAmount ?: 0.0) / targetSum else null
