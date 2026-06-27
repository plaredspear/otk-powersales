package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 여사원 상세 페이지 — 근무이력 항목 (TeamMemberSchedule 기반).
 *
 * 최근 N개를 working_date desc + created_at desc 로 정렬해 반환.
 *
 * 근무기간 조회(월별) 화면에서 "어디서/어떻게 근무했는지"를 함께 표현하기 위해
 * 근무지 폴백(refAccountName)/소속지점(costCenterCode)/부근무유형/근무시간대 필드를 nullable 로 확장.
 * (EmployeeDetailPage 최근이력 응답과 동일 DTO 재사용 — 신규 필드는 모두 nullable 이라 하위호환)
 */
data class EmployeeWorkHistoryItem(
    val id: Long,
    val workingDate: LocalDate?,
    val workingType: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    /** 전문행사조 (SF ProfessionalPromotionTeam__c) — 라면세일조/카레행사조 등. */
    val professionalPromotionTeam: String?,
    val accountName: String?,
    val accountExternalKey: String?,
    /** 거래처유형 (SF Account.Type) — 대형마트(3대)/체인/C.V.S 등. */
    val accountType: String?,
    /** ABC유형 (SF Account.ABCType__c). */
    val abcType: String?,
    /** ABC유형코드 (SF Account.ABCTypeCode__c). */
    val abcTypeCode: String?,
    /** 거래처유형 — ABC유형코드 + ABC유형 조합 (예: "6111 이마트"). Account.abcTypeLabel() 정본. */
    val abcTypeLabel: String?,
    /** 거래처상태코드 (SF Account.AccountStatusCode__c) — 유통형태 표시용. */
    val accountStatusCode: String?,
    /** 유통형태 — 거래처상태코드 + 거래처타입 조합 (예: "02 슈퍼"). Account.distributionChannelLabel() 정본. */
    val distributionChannelLabel: String?,
    val isClockIn: Boolean,
    val refAccountName: String?,
    val costCenterCode: String?,
    val secondWorkType: String?,
    val startTime: LocalDateTime?,
    val completeTime: LocalDateTime?,
) {
    companion object {
        fun from(schedule: TeamMemberSchedule): EmployeeWorkHistoryItem = EmployeeWorkHistoryItem(
            id = schedule.id,
            workingDate = schedule.workingDate,
            workingType = schedule.workingType?.displayName,
            workingCategory1 = schedule.workingCategory1?.displayName,
            workingCategory3 = schedule.workingCategory3?.displayName,
            workingCategory4 = schedule.workingCategory4,
            professionalPromotionTeam = schedule.professionalPromotionTeam,
            accountName = schedule.account?.name,
            accountExternalKey = schedule.account?.externalKey,
            accountType = schedule.account?.accountType,
            abcType = schedule.account?.abcType,
            abcTypeCode = schedule.account?.abcTypeCode,
            abcTypeLabel = schedule.account?.abcTypeLabel(),
            accountStatusCode = schedule.account?.accountStatusCode,
            distributionChannelLabel = schedule.account?.distributionChannelLabel(),
            isClockIn = schedule.attendanceLog != null,
            refAccountName = schedule.refAccountName,
            costCenterCode = schedule.costCenterCode,
            secondWorkType = schedule.secondWorkType,
            startTime = schedule.startTime,
            completeTime = schedule.completeTime,
        )
    }
}

data class EmployeeWorkHistoryResponse(
    val items: List<EmployeeWorkHistoryItem>,
)
