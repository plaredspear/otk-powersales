package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.enums.TypeOfWork1
import java.math.BigDecimal
import java.time.LocalDate

data class EmployeeInputCriteriaMasterResponse(
    val id: Long,
    val name: String?,
    val categoryId: Long?,
    val categoryCode: String?,
    val categoryName: String?,
    val typeOfWork1: TypeOfWork1?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val confirmed: Boolean,
    val boundary: BigDecimal?,
    val fixed1PersonStandardAmount: BigDecimal?,
    val bifurcationHalfPersonStandard: BigDecimal?,
    val fixed1PersonMinAmountInRealmRange: BigDecimal?,
    val bifurcationHalfPersonMinAmountInRealmRange: BigDecimal?,
    val accountCategorizedCode: String?,
    val validData: String?,
) {
    companion object {
        fun from(entity: EmployeeInputCriteriaMaster): EmployeeInputCriteriaMasterResponse =
            EmployeeInputCriteriaMasterResponse(
                id = entity.id,
                name = entity.name,
                categoryId = entity.category?.id,
                categoryCode = entity.category?.accountCode,
                categoryName = entity.category?.name,
                typeOfWork1 = entity.typeOfWork1,
                startDate = entity.startDate,
                endDate = entity.endDate,
                confirmed = entity.confirmed,
                boundary = entity.boundary,
                fixed1PersonStandardAmount = entity.fixed1PersonStandardAmount,
                bifurcationHalfPersonStandard = entity.bifurcationHalfPersonStandard,
                fixed1PersonMinAmountInRealmRange = entity.fixed1PersonMinAmountInRealmRange,
                bifurcationHalfPersonMinAmountInRealmRange = entity.bifurcationHalfPersonMinAmountInRealmRange,
                accountCategorizedCode = entity.accountCategorizedCode,
                validData = entity.validData,
            )
    }
}
