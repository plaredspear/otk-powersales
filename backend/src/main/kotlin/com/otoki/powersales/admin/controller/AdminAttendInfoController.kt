package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoSearchRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendInfoDeleteResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendInfoDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendInfoListItemResponse
import com.otoki.powersales.domain.activity.schedule.service.AdminAttendInfoService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 기준정보 > HR 적재 근무기간 — SAP HR 인바운드 적재 마스터(`attend_info` 테이블) 의 CRUD.
 *
 * ## 권한 자원 — `attend_info` (SF `AttendInfo__c` 매핑 entity 자원)
 *
 * 인사/근무 > 근무기간 **조회** 화면은 본 컨트롤러가 아니라 [AdminWorkHistoryController]
 * (`work_history` 가상 자원) 가 담당한다. 과거에는 두 화면이 본 컨트롤러 하나를 공유해
 * `AttendInfo__c` 를 회수하면 조회 화면까지 함께 닫혔으나, 조회/편집 권한을 분리하기 위해
 * 조회 전용 endpoint (기간별 집계 / 월별 근무내역 / 지점·사원 셀렉터) 를 그쪽으로 옮겼다.
 *
 * 본 컨트롤러에는 적재 마스터 자체의 목록/상세/등록/수정/삭제만 남는다.
 */
@RestController
@RequestMapping("/api/v1/admin/attend-info")
class AdminAttendInfoController(
    private val service: AdminAttendInfoService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "attend_info", operation = SfPermissionOperation.READ)
    fun list(
        @ModelAttribute filter: AdminAttendInfoSearchRequest,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<AdminAttendInfoListItemResponse>>> {
        val pageable = AdminAttendInfoService.normalizePageable(
            PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1))
        )
        return ResponseEntity.ok(ApiResponse.success(service.search(filter, pageable)))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "attend_info", operation = SfPermissionOperation.READ)
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminAttendInfoDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.get(id)))
    }

    @PostMapping
    @RequiresSfPermission(entity = "attend_info", operation = SfPermissionOperation.EDIT)
    fun create(
        @Valid @RequestBody request: AdminAttendInfoCreateRequest,
    ): ResponseEntity<ApiResponse<AdminAttendInfoDetailResponse>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(service.create(request)))
    }

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "attend_info", operation = SfPermissionOperation.EDIT)
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminAttendInfoUpdateRequest,
    ): ResponseEntity<ApiResponse<AdminAttendInfoDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "attend_info", operation = SfPermissionOperation.DELETE)
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminAttendInfoDeleteResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.delete(id)))
    }
}
