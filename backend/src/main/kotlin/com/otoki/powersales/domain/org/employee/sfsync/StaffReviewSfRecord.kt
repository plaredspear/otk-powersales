package com.otoki.powersales.domain.org.employee.sfsync

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SF `IF_SendStaffReviewToPWS` Response 한 건의 raw 역직렬화 표현 ("알라딘 Staffreview 마스터 API" 문서 정합).
 *
 * PDF Response 표의 PascalCase 필드명을 [JsonProperty] 로 바인딩한다. SF 응답에 문서 외 필드가 추가로
 * 오더라도 무시하도록 [JsonIgnoreProperties] `ignoreUnknown=true` 를 둔다.
 *
 * 문서상 모든 필드가 String 타입이라 SF 가 점수/날짜를 문자열로 보낼 수 있으므로, [toFetchDto] 에서
 * 안전 파싱([parseDouble]/[parseDate])하여 [StaffReviewFetchDto] 로 변환한다.
 *
 * upsert 매칭 키는 SF 레코드 [id]([JsonProperty] `Id`). PDF Response 표에 `Id` 가 명시돼 있지 않으나
 * SF Apex REST 응답이 통상 `Id` 를 포함하므로 받아서 sfid 로 매핑한다. SF 응답에 `Id` 가 없으면
 * 해당 row 는 sync 서비스에서 매칭 키 부재로 skip 된다 (운영 응답 확인 후 정정).
 *
 * 근무유형1~3 은 SF picklist 한글 displayName 으로 오므로 enum `fromDisplayNameOrNull` 로 변환한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StaffReviewSfRecord(
    @JsonProperty("Id") val id: String? = null,
    @JsonProperty("Name") val name: String? = null,
    @JsonProperty("DKRetailEmployeeId") val employeeSfid: String? = null,
    @JsonProperty("EmployeeName") val employeeName: String? = null,
    @JsonProperty("EmployeeNumber") val employeeCode: String? = null,
    @JsonProperty("EmployeeType") val employeeType: String? = null,
    @JsonProperty("EntryDate") val entryDate: String? = null,
    @JsonProperty("BranchReviews") val branchReviewSfid: String? = null,
    @JsonProperty("EmployeeTotalScore") val employeeTotalScore: String? = null,
    @JsonProperty("Jikwee") val jikwee: String? = null,
    @JsonProperty("JobCode") val jobCode: String? = null,
    @JsonProperty("FirstDayofMonth") val firstDayOfMonth: String? = null,
    @JsonProperty("Branch") val branch: String? = null,
    @JsonProperty("CostCenterCode") val costCenterCode: String? = null,
    @JsonProperty("DKRetailWorkingCategory1") val workingCategory1: String? = null,
    @JsonProperty("DKRetailWorkingCategory2") val workingCategory2: String? = null,
    @JsonProperty("DKRetailWorkingCategory3") val workingCategory3: String? = null,
    @JsonProperty("DisplayManageEventGoals") val displayEventGoalScore: String? = null,
    @JsonProperty("PriorityEventItemManage") val priorityItemEventScore: String? = null,
    @JsonProperty("ProductManageCallment") val productManageCallmentScore: String? = null,
    @JsonProperty("InstructionsDefault") val instructionDisobedienceScore: String? = null,
    @JsonProperty("BusinessPartnerTies") val accountPartnershipScore: String? = null,
    @JsonProperty("Attendance") val attendanceScore: String? = null,
    @JsonProperty("ClothesSatellite") val clothesHygieneScore: String? = null,
    @JsonProperty("EducationalEvaluation") val educationEvaluationScore: String? = null,
    @JsonProperty("CreatedById") val createdBySfid: String? = null,
    @JsonProperty("LastModifiedById") val lastModifiedBySfid: String? = null,
) {

    /** PDF String 필드 → [StaffReviewFetchDto] (점수는 안전 파싱, 날짜는 ISO/한글 포맷 파싱). */
    fun toFetchDto(): StaffReviewFetchDto = StaffReviewFetchDto(
        sfid = id?.takeIf { it.isNotBlank() },
        name = name?.takeIf { it.isNotBlank() },
        employeeSfid = employeeSfid?.takeIf { it.isNotBlank() },
        employeeName = employeeName?.takeIf { it.isNotBlank() },
        employeeCode = employeeCode?.takeIf { it.isNotBlank() },
        employeeType = employeeType?.takeIf { it.isNotBlank() },
        entryDate = parseDate(entryDate),
        branchReviewSfid = branchReviewSfid?.takeIf { it.isNotBlank() },
        employeeTotalScore = parseDouble(employeeTotalScore),
        jikwee = jikwee?.takeIf { it.isNotBlank() },
        jobCode = jobCode?.takeIf { it.isNotBlank() },
        firstDayOfMonth = parseDate(firstDayOfMonth),
        branch = branch?.takeIf { it.isNotBlank() },
        costCenterCode = costCenterCode?.takeIf { it.isNotBlank() },
        workingCategory1 = WorkingCategory1.fromDisplayNameOrNull(workingCategory1),
        workingCategory2 = WorkingCategory2.fromDisplayNameOrNull(workingCategory2),
        workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(workingCategory3),
        displayEventGoalScore = parseDouble(displayEventGoalScore),
        priorityItemEventScore = parseDouble(priorityItemEventScore),
        productManageCallmentScore = parseDouble(productManageCallmentScore),
        instructionDisobedienceScore = parseDouble(instructionDisobedienceScore),
        accountPartnershipScore = parseDouble(accountPartnershipScore),
        attendanceScore = parseDouble(attendanceScore),
        clothesHygieneScore = parseDouble(clothesHygieneScore),
        educationEvaluationScore = parseDouble(educationEvaluationScore),
        createdBySfid = createdBySfid?.takeIf { it.isNotBlank() },
        lastModifiedBySfid = lastModifiedBySfid?.takeIf { it.isNotBlank() },
    )

    private fun parseDouble(raw: String?): Double? =
        raw?.takeIf { it.isNotBlank() }?.replace(",", "")?.trim()?.toDoubleOrNull()

    /** 날짜 파싱 — ISO(yyyy-MM-dd) / 무구분(yyyyMMdd) 두 포맷을 순서대로 시도. 실패 시 null. */
    private fun parseDate(raw: String?): LocalDate? {
        val value = raw?.takeIf { it.isNotBlank() }?.trim() ?: return null
        for (fmt in DATE_FORMATS) {
            try {
                return LocalDate.parse(value, fmt)
            } catch (_: Exception) {
                // 다음 포맷 시도
            }
        }
        return null
    }

    companion object {
        private val DATE_FORMATS: List<DateTimeFormatter> = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyyMMdd"),
        )
    }
}
