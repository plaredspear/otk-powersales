package com.otoki.powersales.sap.outbound.dto

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SapPPTMasterOutboundDto(
    @JsonProperty("Name")
    val name: String,

    @JsonProperty("ProfessionalPromotionTeam")
    val professionalPromotionTeam: String,

    @JsonProperty("Account")
    val account: String,

    @JsonProperty("FullName")
    val fullName: String,

    @JsonProperty("EmployeeNumber")
    val employeeNumber: String,

    @JsonProperty("AccountStatus")
    val accountStatus: String,

    @JsonProperty("AccountType")
    val accountType: String,

    @JsonProperty("AccountCode")
    val accountCode: String,

    @JsonProperty("StartDate")
    val startDate: String,

    @JsonProperty("EndDate")
    val endDate: String,

    @JsonProperty("ValidData")
    val validData: String,

    @JsonProperty("ValidConditionData")
    val validConditionData: String,

    @JsonProperty("CostCenterCode")
    val costCenterCode: String,

    @JsonProperty("BranchName")
    val branchName: String,

    @JsonProperty("Title")
    val title: String,

    @JsonProperty("Confirmed")
    val confirmed: String,

    @JsonProperty("YearMonth")
    val yearMonth: String
) {
    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val YEAR_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")
        private const val NULL_TOKEN: String = "null"

        fun from(master: ProfessionalPromotionTeamMaster, today: LocalDate): SapPPTMasterOutboundDto {
            val employee = master.employee
            val account = master.account
            return SapPPTMasterOutboundDto(
                name = master.id.toString(),
                professionalPromotionTeam = master.teamType,
                account = account?.name.orEmpty(),
                fullName = employee?.name.orEmpty(),
                employeeNumber = employee?.employeeCode.orEmpty(),
                accountStatus = account?.accountStatusName.orEmpty(),
                accountType = account?.accountType.orEmpty(),
                accountCode = account?.externalKey.orEmpty(),
                startDate = master.startDate.format(DATE_FORMAT),
                endDate = master.endDate?.format(DATE_FORMAT) ?: NULL_TOKEN,
                validData = computeValidData(master.isConfirmed, master.endDate, today),
                validConditionData = employee?.status.orEmpty(),
                costCenterCode = master.branchCode.orEmpty(),
                branchName = master.branchName.orEmpty(),
                title = employee?.jikwee.orEmpty(),
                confirmed = master.isConfirmed.toString(),
                yearMonth = YearMonth.from(today).format(YEAR_MONTH_FORMAT)
            )
        }

        private fun computeValidData(isConfirmed: Boolean, endDate: LocalDate?, today: LocalDate): String {
            if (!isConfirmed) return EXPIRED
            if (endDate == null) return VALID
            return if (!endDate.isBefore(today)) VALID else EXPIRED
        }

        const val VALID: String = "유효"
        const val EXPIRED: String = "만료"
    }
}
