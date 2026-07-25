package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterBulkConfirmRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterFormMetaResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterListMetaResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterResponse
import com.otoki.powersales.domain.activity.schedule.service.AdminEmployeeInputCriteriaMasterService
import com.otoki.powersales.domain.activity.schedule.service.AdminEmployeeInputCriteriaMasterService.ValidStatusFilter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/employee-input-criteria-masters")
class AdminEmployeeInputCriteriaMasterController(
    private val service: AdminEmployeeInputCriteriaMasterService,
) {

    /**
     * 진열사원 투입기준 마스터 폼(등록/수정 모달) 렌더링용 메타.
     *
     * 행사마스터 `/promotions/form-meta` 와 동일한 "form 전용 API 분리" 패턴 —
     * 폼 Select 옵션(구분·근무형태1)을 프론트 상수로 하드코딩하지 않고 서버를 단일 출처로 내려준다.
     * 기존 `/account-categories` lookup 을 흡수한다.
     */
    @GetMapping("/form-meta")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.READ)
    fun getFormMeta(): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterFormMetaResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.getFormMeta()))
    }

    /**
     * 진열사원 투입기준 마스터 목록 화면 조회 조건 로드.
     *
     * 행사마스터 `/promotions/meta` 와 동일한 "조회 조건 로드" 표준 패턴 —
     * 상태 필터(전체/유효/예정/종료) 옵션과 기본값을 내려준다.
     * 본 화면은 지점/권한 의존 조건이 없어(전사 공통 마스터) 서비스 산출을 그대로 반환한다.
     */
    @GetMapping("/meta")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.READ)
    fun getListMeta(): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterListMetaResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.getListMeta()))
    }

    @GetMapping
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.READ)
    fun list(
        @RequestParam(name = "status", required = false, defaultValue = "ALL") status: ValidStatusFilter,
    ): ResponseEntity<ApiResponse<List<EmployeeInputCriteriaMasterResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(service.list(status)))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.READ)
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.get(id)))
    }

    @PostMapping
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun create(
        @Valid @RequestBody request: EmployeeInputCriteriaMasterCreateRequest,
    ): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterResponse>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(service.create(request)))
    }

    /**
     * 수정. 확정된 레코드는 종료일만 변경 가능하며(SF ValidationRule `EditDisableForEmployeeMaster` 동등),
     * 시스템 관리자만 그 제한의 예외다.
     */
    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: EmployeeInputCriteriaMasterUpdateRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterResponse>> {
        val isSystemAdmin = SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request, isSystemAdmin)))
    }

    @PostMapping("/{id}/confirm")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun confirm(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterResponse>> {
        requireSystemAdminForConfirm(principal)
        return ResponseEntity.ok(ApiResponse.success(service.confirm(id)))
    }

    @PostMapping("/bulk-confirm")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun bulkConfirm(
        @Valid @RequestBody request: EmployeeInputCriteriaMasterBulkConfirmRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<EmployeeInputCriteriaMasterResponse>>> {
        requireSystemAdminForConfirm(principal)
        return ResponseEntity.ok(ApiResponse.success(service.bulkConfirm(request.ids)))
    }

    /**
     * 확정 권한 가드 — 확정은 수정과 분리된 시스템 관리자 전용 액션이다(사용자 결정).
     *
     * SF 권한 모델의 entity × operation 은 `object_permissions` 4비트(R/C/E/D) 에 1:1 매핑이라
     * "확정" 이라는 다섯 번째 축을 표현할 수 없다. 기능 활성화 / 로그 레벨 관리와 동일하게
     * [SystemAdminProfilePolicy.isSystemAdmin] 으로 컨트롤러에서 직접 판정한다.
     * (`@RequiresSfPermission` 의 EDIT 는 유지 — 마스터 접근 자체의 최소 요건.)
     */
    private fun requireSystemAdminForConfirm(principal: WebUserPrincipal) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)) {
            throw BusinessException(
                errorCode = "EMPLOYEE_INPUT_CRITERIA_CONFIRM_DENIED",
                message = "확정은 시스템 관리자만 수행할 수 있습니다",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.DELETE)
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Any?>> {
        service.delete(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
