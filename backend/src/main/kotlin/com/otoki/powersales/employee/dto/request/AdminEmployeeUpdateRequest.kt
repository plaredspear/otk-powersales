package com.otoki.powersales.employee.dto.request

import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.employee.enums.CrmWorkType
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 사원 정보 수정 요청 DTO (UC-07 web admin 단건 수정).
 *
 * 사원번호(employeeCode) / 사원명(name) 은 변경 불가 — 수정 불가능 필드는 본 DTO 에서 제외.
 *
 * 모든 필드는 nullable — partial update 의미 (null = "값 변경 없음" 이 아니라 "null 로 갱신" 이 아닌
 * PATCH semantics. 신규에서는 명시적 nullification 이 필요하면 별도 endpoint 로 분리한다).
 * 본 PATCH 는 "비어 있지 않은 필드만 갱신" 방식으로 처리한다 — 빈 문자열은 trim 후 빈 값으로 취급.
 *
 * `professionalPromotionTeam` 은 직무코드 판촉직·레이디직·OSC직 사원에 한해 ProfessionalPromotionTeamType
 * 허용값으로 검증 (Service 단에서 발화).
 */
data class AdminEmployeeUpdateRequest(

    val status: String? = null,

    val role: UserRoleEnum? = null,

    @field:Size(max = 100, message = "조직명은 100자 이하여야 합니다")
    val orgName: String? = null,

    @field:Size(max = 10, message = "지점코드는 10자 이하여야 합니다")
    val costCenterCode: String? = null,

    @field:Size(max = 100, message = "근무지역은 100자 이하여야 합니다")
    val workArea: String? = null,

    @field:Size(max = 100, message = "위치코드는 100자 이하여야 합니다")
    val locationCode: String? = null,

    @field:Size(max = 40, message = "직무코드는 40자 이하여야 합니다")
    val jobCode: String? = null,

    @field:Size(max = 40, message = "직종은 40자 이하여야 합니다")
    val jikjong: String? = null,

    @field:Size(max = 40, message = "직위는 40자 이하여야 합니다")
    val jikwee: String? = null,

    @field:Size(max = 100, message = "직책은 100자 이하여야 합니다")
    val jikchak: String? = null,

    @field:Size(max = 40, message = "직급은 40자 이하여야 합니다")
    val jikgub: String? = null,

    @field:Size(max = 40, message = "근무형태는 40자 이하여야 합니다")
    val workType: String? = null,

    @field:Size(max = 255, message = "발령명은 255자 이하여야 합니다")
    val ordDetailNode: String? = null,

    val appointmentDate: LocalDate? = null,

    val startDate: LocalDate? = null,

    val endDate: LocalDate? = null,

    @field:Size(max = 40, message = "전화번호는 40자 이하여야 합니다")
    val phone: String? = null,

    @field:Size(max = 255, message = "집 전화번호는 255자 이하여야 합니다")
    val homePhone: String? = null,

    @field:Size(max = 255, message = "업무 전화는 255자 이하여야 합니다")
    val workPhone: String? = null,

    @field:Size(max = 40, message = "사무실 전화는 40자 이하여야 합니다")
    val officePhone: String? = null,

    @field:Email(message = "업무 이메일 형식이 올바르지 않습니다")
    @field:Size(max = 100, message = "업무 이메일은 100자 이하여야 합니다")
    val workEmail: String? = null,

    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    @field:Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    val email: String? = null,

    val appLoginActive: Boolean? = null,

    val lockingFlag: Boolean? = null,

    val professionalPromotionTeam: ProfessionalPromotionTeamType? = null,

    val crmWorkType: CrmWorkType? = null,

    val crmWorkStartDate: LocalDate? = null,

    val totalAnnualLeave: BigDecimal? = null,

    val usedAnnualLeave: BigDecimal? = null
)
