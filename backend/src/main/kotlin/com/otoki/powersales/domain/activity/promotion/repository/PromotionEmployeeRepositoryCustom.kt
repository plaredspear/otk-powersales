package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.enums.StandLocation
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.querydsl.core.types.Predicate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface PromotionEmployeeRepositoryCustom {

    /**
     * SF Sharing Rule 정책이 합성된 가시 PromotionEmployee 일람 (spec #782 P4-B — ControlledByParent).
     *
     * PromotionEmployee 의 sharingModel = ControlledByParent (parent = Promotion). Service layer 가
     * 부모 Promotion entity 기준 [SharingRulePolicyEvaluator.buildPredicate] 호출 결과를 [parentPolicyPredicate] 로 전달.
     * Repository 는 `join(promotionEmployee.promotion, promotion).where(parentPolicyPredicate)` 합성.
     */
    fun findAllAccessibleByParentPolicy(parentPolicyPredicate: Predicate): List<PromotionEmployee>

    fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee>

    fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate?

    /**
     * 다수 행사마스터의 목표/실적금액 일괄 집계 — SF rollup 재현 (단건 상세도 1건 리스트로 재사용).
     * - 목표금액 = SUM(조원 `dailyTargetCount * basePrice`) = SF `DKRetail__TargetAmount__c` 동등.
     * - 실적금액 = SUM(조원 `primaryProductAmount + otherSalesAmount`) = SF `ActualAmount__c`(총실적 rollup) 동등.
     * entity 저장 컬럼 target_amount/actual_amount 및 Promotion 의 dk*Amount 동기화 스칼라는 stale 이므로 미사용.
     * 반환: promotionId -> Pair(목표금액 합, 실적금액 합). 조원이 없는 promotionId 는 맵에 미포함.
     */
    fun sumTargetActualAmountByPromotionIds(promotionIds: Collection<Long>): Map<Long, Pair<Long, Long>>

    fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate?

    /**
     * 행사사원 목표 대비 실적 보고서 조회 (Spec #845 — SF Report `new_report_AtQ` 이식).
     * `promotion_employee` ⋈ promotion ⋈ promotion.account ⋈ promotion.primaryProduct ⋈ employee ⋈ teamMemberSchedule ⋈ attendanceLog.
     * 필터: scheduleDate ∈ [startDate, endDate], soft-delete 제외.
     * 지점 스코프: branchScopeCodes 비어있지 않으면 여사원일정 소속 지점(teamMemberSchedule.costCenterCode) IN. 빈 목록 = 전사.
     *   teamMemberSchedule 은 leftJoin 이라 미연결 행은 스코프 적용 시 제외된다(지점 판별 불가).
     * 정렬: 행사명(promotion.promotionNumber = SF Name) 오름차순 + scheduleDate 오름차순 (Summary 그룹 재현).
     *
     * DTO projection — 보고서에 쓰는 컬럼만 select (entity fetchJoin 대비 행 폭 ~1/8, hydration 회피).
     * isWorkReport/commuteDate 는 TeamMemberSchedule 파생 프로퍼티가 attendanceLog LAZY 관계를 읽는 구조라
     * entity 반환 시 행마다 N+1 로딩이 발생 → attendanceLog 를 조인에 포함해 컬럼으로 직접 select 한다.
     */
    fun findTargetActualReport(
        startDate: LocalDate,
        endDate: LocalDate,
        branchScopeCodes: List<String>,
    ): List<PromotionTargetActualReportRecord>

    /**
     * 로그인 여사원의 특정 일자 담당 행사 일람 (홈 "행사매출 등록" → 일 매출 등록 진입화면용).
     * 레거시 Heroku `eventlistapi`(EmployeeCode + StartDate=EndDate=today) 동등.
     * `promotion_employee` ⋈ promotion ⋈ promotion.account (fetchJoin).
     * 필터: employeeId == me, scheduleDate == date, soft-delete 제외, promotion soft-delete 제외.
     * 정렬: promotion.promotionNumber 오름차순.
     */
    fun findMyAssignmentsByDate(employeeId: Long, date: LocalDate): List<PromotionEmployee>

    /**
     * 로그인 여사원이 **확정된 행사사원**으로 등록된, **[date] 당시 유효한 기간**의 행사마스터 거래처 id 일람
     * (주문서 작성 화면 거래처 후보의 행사 축).
     *
     * 필터:
     * - `employeeId == me` — 본인이 행사사원으로 등록
     * - `teamMemberScheduleId IS NOT NULL` — **확정 여부**. 이 백링크는 행사 확정
     *   ([com.otoki.powersales.domain.activity.promotion.service.PromotionSchedulesUpsertHelper.upsert])
     *   시점에만 채워지므로 미확정 행사의 행사사원은 제외된다. 판정 단위는 행사가 아니라 **행사사원 개인**이라,
     *   확정 이후 추가 등록되어 아직 백링크가 없는 row 는 같은 행사라도 제외된다.
     * - `promotion.startDate <= date <= promotion.endDate` — 행사마스터 기간이 [date] 를 포함.
     *   두 날짜는 nullable(SF nillable 정합)이나 확정 시 필수 검증을 통과한 행사는 값이 채워져 있다.
     * - 행사사원/행사마스터 soft-delete 제외, 거래처 미지정 행사 제외.
     *
     * 여사원 개인 투입일(`scheduleDate`)이 아니라 **행사마스터 기간**을 쓰므로, 3일 행사에 1일차만 투입된
     * 여사원도 2·3일차에 해당 거래처를 후보로 갖는다.
     */
    fun findConfirmedAssignedAccountIdsByEmployeeAndDate(employeeId: Long, date: LocalDate): List<Long>

    /**
     * 행사마스터의 행사사원 일람 — soft-delete(IsDeleted) 제외 (SF 정합).
     *
     * SF 는 행사사원 조회 시 표준 SOQL 기본 동작으로 IsDeleted=true row 를 항상 제외한다.
     * 복제 / cascade 삭제 / 스케줄 upsert 등 promotionId 기준 전 조회가 본 메서드를 공유.
     */
    fun findByPromotionId(promotionId: Long): List<PromotionEmployee>

    /** 마감(PromoCloseByTm) 행사사원 존재 여부 — soft-delete 제외 (SF 정합). */
    fun existsByPromotionIdAndPromoCloseByTmTrue(promotionId: Long): Boolean

    /** 행사사원 중복 등록 여부 — soft-delete 제외 (SF 정합). */
    fun existsByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): Boolean
}

/**
 * 목표 대비 실적 보고서 1행 projection ([PromotionEmployeeRepositoryCustom.findTargetActualReport]).
 *
 * SF Formula 컬럼 2종은 원천 컬럼을 select 해 파생 프로퍼티로 재현한다 (entity 파생 getter 와 동일 null 의미론):
 * - [targetAmount] = `DKRetail__DailyTargetAmount__c` (목표갯수×기준단가)
 * - [actualAmount] = `DailyActualSalesAmount__c` (총 실적 = 대표금액+기타금액)
 */
data class PromotionTargetActualReportRecord(
    val promotionName: String?,
    val branchName: String?,
    val accountName: String?,
    /** SF AccCode__c = AccId__r.ExternalKey__c (SAP 거래처코드). */
    val accountCode: String?,
    val primaryProductName: String?,
    /** SF formula Category1__c = 대표제품 storeConditionText. */
    val category1: String?,
    val otherProduct: String?,
    val standLocation: StandLocation?,
    val employeeCode: String?,
    val employeeOrgName: String?,
    val employeeName: String?,
    /** 사원 마스터의 현재 소속 조 (SF 임철민팀장용 변형 "전문행사조(현재)"). */
    val professionalPromotionTeamCurrent: ProfessionalPromotionTeamType?,
    /** 조원일정에 기록된 투입 당시 값 (SF 영업지원실용 "전문행사조"). */
    val professionalPromotionTeam: String?,
    val scheduleDate: LocalDate?,
    val dailyTargetCount: BigDecimal?,
    val basePrice: BigDecimal?,
    val primarySalesQuantity: BigDecimal?,
    val primaryProductAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val workType2: WorkingCategory2?,
    val workType3: WorkingCategory3?,
    /** 출퇴근 로그 FK — isWorkReport(존재 시 "근무등록") 판정용. */
    val attendanceLogId: Long?,
    /** 출퇴근 로그의 출근일시 (SF formula DKRetail__CommuteDate__c). */
    val commuteDate: LocalDateTime?,
) {

    /** SF Formula `DKRetail__DailyTargetAmount__c` 재현 — PromotionEmployee.dkDailyTargetAmount 와 동일 공식. */
    val targetAmount: BigDecimal?
        get() {
            if (dailyTargetCount == null && basePrice == null) return null
            return (dailyTargetCount ?: BigDecimal.ZERO) * (basePrice ?: BigDecimal.ZERO)
        }

    /** SF Formula `DailyActualSalesAmount__c`(총 실적) 재현 — PromotionEmployee.dailyTotalActualSalesAmount 와 동일 공식. */
    val actualAmount: BigDecimal?
        get() {
            if (primaryProductAmount == null && otherSalesAmount == null) return null
            return (primaryProductAmount ?: BigDecimal.ZERO) + (otherSalesAmount ?: BigDecimal.ZERO)
        }

    /** SF formula `isworkreport__c` 재현 — 출퇴근 로그 존재 시 "근무등록", 부재 시 빈 문자열. */
    val isWorkReport: String
        get() = if (attendanceLogId != null) "근무등록" else ""
}
