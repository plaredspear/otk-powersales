package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.org.organization.branchmapping.dto.response.BranchMappingListResponse
import com.otoki.powersales.domain.org.organization.branchmapping.service.AdminBranchMappingService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 지점 코드 맵핑 조회 컨트롤러 — `시스템 > 지점 코드 맵핑` 화면 (조회 전용).
 *
 * `BranchCodeExpander` 가 지점 스코프 조회에서 코드를 어떻게 확장하는지 확인하는 진단 화면이다.
 * 운영 데이터가 아니라 시스템 내부 매핑을 보여주므로, entity 단위 권한이 아니라 시스템 권한
 * (`VIEW_ALL_DATA`) 으로 가드한다 — 같은 시스템 메뉴 그룹의 `권한 매트릭스` / `페이지별 필요 권한`
 * (`AdminScheduledJobController` 계열) 과 동일 방식.
 */
@RestController
@RequestMapping("/api/v1/admin/branch-mappings")
class AdminBranchMappingController(
    private val adminBranchMappingService: AdminBranchMappingService,
) {

    /**
     * 지점 코드 맵핑 전건 조회 (74건 규모라 페이징 없음).
     *
     * @param keyword 지점코드 / 라벨 / 조직명 / 확장 코드를 동시에 대상으로 하는 부분 일치 검색어.
     *   확장 코드까지 매칭하므로 "이 코드를 포함하는 매핑 행" 역방향 조회가 같은 입력으로 가능하다.
     */
    @GetMapping
    @RequiresSfPermission(
        operation = SfPermissionOperation.SYSTEM,
        systemPermission = SfSystemPermission.VIEW_ALL_DATA,
    )
    fun getBranchMappings(
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<ApiResponse<BranchMappingListResponse>> {
        if (keyword != null && keyword.length > 50) {
            throw IllegalArgumentException("검색어는 50자 이하여야 합니다")
        }
        return ResponseEntity.ok(ApiResponse.success(adminBranchMappingService.getBranchMappings(keyword)))
    }
}
