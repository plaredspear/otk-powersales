package com.otoki.powersales.admin.tools.feature.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 화면 요청/응답 DTO.
 */

/** 예외 사원 1건 (비활성 상태에서도 등록이 허용되는 사원). */
data class FeatureToggleExemptEmployee(
    /** 사번 (employee.employee_code). */
    val employeeCode: String,
    /** 사원명. 사번이 등록 후 삭제된 사원이면 null. */
    val name: String?,
)

/** 개별 기능 토글 상태. */
data class FeatureToggleItem(
    /** 기능 식별자 (안정 code). */
    val code: String,
    /** 화면 표시용 한글명. */
    val label: String,
    /** 현재 활성 여부. */
    val enabled: Boolean,
    /** 비활성 사유 (관리자 입력, 활성이거나 미입력이면 null). */
    val reason: String?,
    /** 예외 사원 목록 (사번 오름차순). 비활성이어도 이 사원들은 등록할 수 있다. */
    val exemptEmployees: List<FeatureToggleExemptEmployee>,
)

/** 기능 토글 목록 응답. */
data class FeatureToggleListResponse(
    val features: List<FeatureToggleItem>,
)

/**
 * 기능 토글 변경 요청.
 *
 * `reason` 은 비활성화(`enabled=false`) 시 모바일에 노출되는 사유 문구다. 활성화 시에는 무시된다.
 */
data class UpdateFeatureToggleRequest(
    @field:NotBlank(message = "기능 code 는 필수입니다")
    val code: String,
    val enabled: Boolean,
    @field:Size(max = 200, message = "사유는 200자 이하로 입력하세요")
    val reason: String?,
)

/** 예외 사원 추가 요청. 존재하지 않는 사번이면 400 으로 거부된다. */
data class AddFeatureToggleExemptEmployeeRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Size(max = 100, message = "사번은 100자 이하로 입력하세요")
    val employeeCode: String,
)
