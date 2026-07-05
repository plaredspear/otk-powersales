package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 매출/실적 계열 화면(월 매출 물류배부·전산실적·POS·월별 투입적합성·배치 적합성) 공용 지점 셀렉터 endpoint.
 *
 * 이 화면들은 모두 `monthly_sales_history` READ 로 게이팅되므로, 지점 셀렉터도 동일 entity 로 가드해
 * 화면 게이팅 권한과 API 가드를 정합시킨다. 여사원 일정관리의 `/team-schedule/branches`
 * (`team_member_schedule` 가드) 를 빌려쓰면 `team_member_schedule` READ 없는 사용자가 지점 셀렉터에서
 * 403 이 나므로 분리한다.
 *
 * 지점 화이트리스트 산출 로직은 여사원 일정/현황/전문행사조와 동일 단일 출처
 * ([WomenScheduleBranchResolver]) 를 재사용한다 — 반환 지점 범위는 사용자 권한
 * (SYSTEM_ADMIN / ALL_BRANCHES / 본인 지점) 기준으로 제한된다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales")
class AdminSalesBranchController(
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {

    @GetMapping("/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = womenScheduleBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
