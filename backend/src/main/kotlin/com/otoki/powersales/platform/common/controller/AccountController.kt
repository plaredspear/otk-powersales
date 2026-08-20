package com.otoki.powersales.platform.common.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.MyAccountListResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.platform.common.service.MyAccountScope
import com.otoki.powersales.platform.common.service.MyAccountService
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 거래처 API Controller
 */
@RestController
@RequestMapping("/api/v1/mobile/accounts")
class AccountController(
    private val myAccountService: MyAccountService
) {

    /**
     * 내 거래처 목록 조회
     * GET /api/v1/mobile/accounts/my
     *
     * 권한(여사원/조장/부서장)에 따라 레거시 거래처 조회 분기를 재현한다.
     * keyword 파라미터로 거래처명/거래처코드 검색 가능.
     *
     * scope 값 (미지정/미지의 값은 field):
     * - `sales` : 매출 계열(POS/전산/월매출). 이 값에서만 부서장 전체조회 분기가 동작한다.
     * - `field` : 현장 활동 계열(기본값).
     * - `order` : 주문 계열(작성/조회 필터). 팀멤버스케줄 ∪ 진열 일정 + 주문가능 거래처유형.
     *
     * `purpose=write` 를 함께 보내면 주문서 **작성** 기준으로 좁힌다 — 여사원 경로 한정으로
     * 오늘 확정된 근무(진열마스터 ∪ 확정 행사) 거래처만 (개발자 도구 > 기능 활성화 의
     * `ORDER_ACCOUNT_DISPLAY_SCHEDULE_ONLY` 비활성 시 `order` 와 동일). 별도 scope 값이 아니라
     * 부가 파라미터인 이유는 [MyAccountScope.from] KDoc 참조 (앱↔서버 버전 스큐 시 안전 폴백).
     */
    @GetMapping("/my")
    fun getMyAccounts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @Size(max = 100) keyword: String?,
        @RequestParam(required = false) scope: String?,
        @RequestParam(required = false) purpose: String?
    ): ResponseEntity<ApiResponse<MyAccountListResponse>> {
        val response =
            myAccountService.getMyAccounts(principal.userId, keyword, MyAccountScope.from(scope, purpose))
        return ResponseEntity.ok(ApiResponse.success(response, "내 거래처 목록 조회 성공"))
    }
}
