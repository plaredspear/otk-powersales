package com.otoki.powersales.admin.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 시스템 관리자 수동 등록 요청 DTO (Spec #579).
 *
 * - employeeCode 는 `ADMIN-` prefix 강제
 * - role 필드 없음 (백엔드에서 항상 `SYSTEM_ADMIN` 으로 저장)
 * - password / passwordConfirm 일치 및 정책 검증은 서비스 레이어에서 수행
 */
data class AdminEmployeeRegisterRequest(

    @field:NotBlank(message = "사번은 필수입니다")
    @field:Pattern(
        regexp = "^ADMIN-[A-Za-z0-9_-]{1,30}$",
        message = "사번은 'ADMIN-' 으로 시작해야 합니다"
    )
    val employeeCode: String,

    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 80, message = "이름은 80자 이하여야 합니다")
    val name: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다")
    val password: String,

    @field:NotBlank(message = "비밀번호 확인은 필수입니다")
    @field:Size(min = 8, max = 64, message = "비밀번호 확인은 8자 이상 64자 이하여야 합니다")
    val passwordConfirm: String,

    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    @field:Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    val workEmail: String? = null,

    @field:Size(max = 30, message = "사무실 전화는 30자 이하여야 합니다")
    val workPhone: String? = null,

    @field:Size(max = 100, message = "조직명은 100자 이하여야 합니다")
    val orgName: String? = null,

    @field:Size(max = 10, message = "코스트센터 코드는 10자 이하여야 합니다")
    val costCenterCode: String? = null
)
