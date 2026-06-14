package com.otoki.powersales.domain.activity.schedule.dto.request

import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDate

data class EmployeeInputCriteriaMasterCreateRequest(
    @field:NotNull(message = "구분(거래처유형마스터) 은 필수입니다")
    val categoryId: Long,

    val typeOfWork1: TypeOfWork1? = null,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    @field:NotNull(message = "경계율은 필수입니다")
    @field:PositiveOrZero(message = "경계율은 0 이상이어야 합니다")
    val boundary: BigDecimal,

    @field:NotNull(message = "고정1명 기준금액은 필수입니다")
    @field:PositiveOrZero(message = "고정1명 기준금액은 0 이상이어야 합니다")
    val fixed1PersonStandardAmount: BigDecimal,

    @field:NotNull(message = "격고0.5명 기준금액은 필수입니다")
    @field:PositiveOrZero(message = "격고0.5명 기준금액은 0 이상이어야 합니다")
    val bifurcationHalfPersonStandard: BigDecimal,
)

data class EmployeeInputCriteriaMasterUpdateRequest(
    @field:NotNull(message = "구분(거래처유형마스터) 은 필수입니다")
    val categoryId: Long,

    val typeOfWork1: TypeOfWork1? = null,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    @field:NotNull(message = "경계율은 필수입니다")
    @field:PositiveOrZero(message = "경계율은 0 이상이어야 합니다")
    val boundary: BigDecimal,

    @field:NotNull(message = "고정1명 기준금액은 필수입니다")
    @field:PositiveOrZero(message = "고정1명 기준금액은 0 이상이어야 합니다")
    val fixed1PersonStandardAmount: BigDecimal,

    @field:NotNull(message = "격고0.5명 기준금액은 필수입니다")
    @field:PositiveOrZero(message = "격고0.5명 기준금액은 0 이상이어야 합니다")
    val bifurcationHalfPersonStandard: BigDecimal,
)

data class EmployeeInputCriteriaMasterBulkConfirmRequest(
    @field:NotNull(message = "id 목록은 필수입니다")
    val ids: List<Long>,
)
