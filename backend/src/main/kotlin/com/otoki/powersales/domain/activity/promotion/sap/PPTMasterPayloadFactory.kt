package com.otoki.powersales.domain.activity.promotion.sap

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 전문행사조 마스터 SAP 송신 페이로드 빌더 (Spec #765 §6.1).
 *
 * 레거시 `IF_REST_SAP_PPTMToSAP.cls:99-124` 의 paraMap 17개 키 변환 로직 1:1 정합. SF retrieve 메타의 수식 필드
 * 정의를 reveal 하여 출처를 직원/거래처 entity 의 원본 필드에 매핑한다.
 *
 * - `ValidData` / `ValidConditionData` 는 SF 수식 (`field-meta.xml` 정의) 을 Kotlin 으로 재현 (§6.1.1 / §6.1.2)
 * - `EndDate` 가 null 인 경우 `"null"` 문자열로 직렬화 (레거시 `String.valueOf(null)` 정합 — Q1 노선 A 버그 재현 일관성)
 * - `Valid` key 는 미송신 (Q5 — 레거시 주석처리 dead 수용)
 */
@Component
class PPTMasterPayloadFactory {

    fun build(masters: List<ProfessionalPromotionTeamMaster>, now: LocalDate = LocalDate.now()): PPTMasterSapPayload {
        return PPTMasterSapPayload(
            REQUEST = masters.map { toRow(it, now) }
        )
    }

    private fun toRow(master: ProfessionalPromotionTeamMaster, today: LocalDate): PPTMasterSapPayloadRow {
        val emp = master.employee
        val acc = master.account
        return PPTMasterSapPayloadRow(
            Name = master.name,
            ProfessionalPromotionTeam = master.teamType.displayName,
            // 레거시 Account__c(SF Account sfid) 자리 — 신규 거래처 PK 를 문자열로 송신.
            Account = acc?.id?.toString(),
            FullName = emp?.name,
            EmployeeNumber = emp?.employeeCode,
            AccountStatus = acc?.accountStatusName,
            AccountType = acc?.accountType?.displayName,
            AccountCode = acc?.externalKey,
            StartDate = master.startDate.toString(),
            EndDate = master.endDate?.toString() ?: LEGACY_NULL_DATE,
            ValidData = computeValidData(master, today),
            ValidConditionData = computeValidConditionData(emp, today),
            CostCenterCode = master.branchCode,
            BranchName = emp?.orgName,
            Title = emp?.jikwee,
            Confirmed = master.isConfirmed.toString(),
            YearMonth = today.format(YEAR_MONTH_FORMATTER)
        )
    }

    /**
     * 레거시 `ValidData__c.field-meta.xml` 수식 재현 (Spec #765 §6.1.1).
     *
     * `IF(Confirmed__c == false, "미확정", IF(StartDate <= TODAY AND (EndDate >= TODAY OR EndDate IS NULL), "유효",
     *   IF(StartDate > TODAY AND (EndDate >= TODAY OR EndDate IS NULL), "예정", "종료")))`
     */
    private fun computeValidData(master: ProfessionalPromotionTeamMaster, today: LocalDate): String {
        if (!master.isConfirmed) return "미확정"
        val endDateOk = master.endDate == null || !master.endDate!!.isBefore(today)
        return when {
            !master.startDate.isAfter(today) && endDateOk -> "유효"
            master.startDate.isAfter(today) && endDateOk -> "예정"
            else -> "종료"
        }
    }

    /**
     * 레거시 `ValidConditionData__c.field-meta.xml` 수식 재현 (Spec #765 §6.1.2).
     *
     * `IF((Status = "퇴직" OR APPLoginActive = false) AND EndDate < TODAY, "퇴직" + EndDate,
     *   IF((Status = "퇴직" OR APPLoginActive = false) AND EndDate > TODAY, "퇴직예정" + EndDate,
     *     IF(Status = "휴직", "휴직", "재직")))`. emp 가 null 이면 default 분기 "재직" 반환.
     */
    private fun computeValidConditionData(emp: Employee?, today: LocalDate): String {
        if (emp == null) return "재직"
        val isResigned = emp.status == "퇴직" || emp.appLoginActive == false
        val empEndDate = emp.endDate
        if (isResigned && empEndDate != null && empEndDate.isBefore(today)) {
            return "퇴직$empEndDate"
        }
        if (isResigned && empEndDate != null && empEndDate.isAfter(today)) {
            return "퇴직예정$empEndDate"
        }
        if (emp.status == "휴직") return "휴직"
        return "재직"
    }

    companion object {
        private const val LEGACY_NULL_DATE: String = "null"
        private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")
    }
}
