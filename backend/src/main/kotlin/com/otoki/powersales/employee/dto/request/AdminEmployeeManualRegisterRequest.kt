package com.otoki.powersales.employee.dto.request

import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 일반 사원 수동 등록 요청 DTO (UC-06 web admin manual 등록).
 *
 * `AdminEmployeeRegisterRequest` (ADMIN-prefix SYSTEM_ADMIN 등록 전용) 와 별개. 본 DTO 는 운영 사원
 * (여사원·조장·영업사원 등) 의 manual 등록을 위한 것. employeeCode 는 6자리 숫자 또는 영업 조직 운영
 * 규칙에 따라 자유 형식.
 *
 * - origin 은 항상 MANUAL 로 고정 저장 (SAP 인입 갱신 회피)
 * - role 은 SYSTEM_ADMIN 외 모든 운영 역할 허용 — SYSTEM_ADMIN 은 별도 ADMIN-prefix endpoint 사용
 * - 비밀번호는 본 endpoint 에서 받지 않음. 등록 후 별도 [POST /{id}/reset-password] 로 임시 비밀번호 설정
 */
data class AdminEmployeeManualRegisterRequest(

    @field:NotBlank(message = "사번은 필수입니다")
    @field:Pattern(
        regexp = "^[A-Za-z0-9_-]{1,20}$",
        message = "사번은 영문·숫자·하이픈·언더스코어 20자 이하여야 합니다"
    )
    val employeeCode: String,

    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 80, message = "이름은 80자 이하여야 합니다")
    val name: String,

    val role: UserRoleEnum? = null,

    @field:Size(max = 100, message = "조직명은 100자 이하여야 합니다")
    val orgName: String? = null,

    @field:Size(max = 10, message = "지점코드는 10자 이하여야 합니다")
    val costCenterCode: String? = null,

    @field:Size(max = 40, message = "직무코드는 40자 이하여야 합니다")
    val jobCode: String? = null,

    @field:Size(max = 40, message = "직위는 40자 이하여야 합니다")
    val jikwee: String? = null,

    @field:Size(max = 100, message = "직책은 100자 이하여야 합니다")
    val jikchak: String? = null,

    @field:Size(max = 40, message = "직급은 40자 이하여야 합니다")
    val jikgub: String? = null,

    val startDate: LocalDate? = null,

    @field:Size(max = 255, message = "집 전화번호는 255자 이하여야 합니다")
    val homePhone: String? = null,

    @field:Size(max = 255, message = "업무 전화는 255자 이하여야 합니다")
    val workPhone: String? = null,

    @field:Email(message = "업무 이메일 형식이 올바르지 않습니다")
    @field:Size(max = 100, message = "업무 이메일은 100자 이하여야 합니다")
    val workEmail: String? = null,

    val professionalPromotionTeam: ProfessionalPromotionTeamType? = null
)
