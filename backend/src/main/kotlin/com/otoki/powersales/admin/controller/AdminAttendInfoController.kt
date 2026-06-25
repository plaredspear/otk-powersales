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
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberDto
import com.otoki.powersales.domain.activity.schedule.service.AdminAttendInfoService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
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

@RestController
@RequestMapping("/api/v1/admin/attend-info")
class AdminAttendInfoController(
    private val service: AdminAttendInfoService,
) {

    /**
     * 근무기간 조회 화면 좌측 여사원 선택 목록.
     *
     * 여사원 일정관리의 /team-schedule/form members 를 빌려쓰던 것을 화면 도메인 권한(attend_info)으로
     * 분리. 퇴사/휴직 등 비활성 여사원도 포함하여 과거 근무내역 조회를 지원한다.
     */
    @GetMapping("/members")
    @RequiresSfPermission(entity = "attend_info", operation = SfPermissionOperation.READ)
    fun getMembers(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<TeamMemberDto>>> {
        return ResponseEntity.ok(ApiResponse.success(service.getMembers(principal)))
    }

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
