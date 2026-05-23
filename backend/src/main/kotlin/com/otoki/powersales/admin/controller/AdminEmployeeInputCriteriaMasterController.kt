package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.schedule.dto.request.EmployeeInputCriteriaMasterBulkConfirmRequest
import com.otoki.powersales.schedule.dto.request.EmployeeInputCriteriaMasterCreateRequest
import com.otoki.powersales.schedule.dto.request.EmployeeInputCriteriaMasterUpdateRequest
import com.otoki.powersales.schedule.dto.response.EmployeeInputCriteriaMasterResponse
import com.otoki.powersales.schedule.service.AdminEmployeeInputCriteriaMasterService
import com.otoki.powersales.schedule.service.AdminEmployeeInputCriteriaMasterService.ValidStatusFilter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class AccountCategoryOptionResponse(
    val id: Long,
    val accountCode: String,
    val name: String,
)

@RestController
@RequestMapping("/api/v1/admin/employee-input-criteria-masters")
class AdminEmployeeInputCriteriaMasterController(
    private val service: AdminEmployeeInputCriteriaMasterService,
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository,
) {

    @GetMapping("/account-categories")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.READ)
    fun listAccountCategories(): ResponseEntity<ApiResponse<List<AccountCategoryOptionResponse>>> {
        val categories = accountCategoryMasterRepository.findAll()
            .filter { it.isDeleted != true }
            .sortedBy { it.accountCode }
            .map { AccountCategoryOptionResponse(it.id, it.accountCode, it.name) }
        return ResponseEntity.ok(ApiResponse.success(categories))
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

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: EmployeeInputCriteriaMasterUpdateRequest,
    ): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)))
    }

    @PostMapping("/{id}/confirm")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun confirm(@PathVariable id: Long): ResponseEntity<ApiResponse<EmployeeInputCriteriaMasterResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.confirm(id)))
    }

    @PostMapping("/bulk-confirm")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun bulkConfirm(
        @Valid @RequestBody request: EmployeeInputCriteriaMasterBulkConfirmRequest,
    ): ResponseEntity<ApiResponse<List<EmployeeInputCriteriaMasterResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(service.bulkConfirm(request.ids)))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "employee_input_criteria_master", operation = SfPermissionOperation.EDIT)
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Any?>> {
        service.delete(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
