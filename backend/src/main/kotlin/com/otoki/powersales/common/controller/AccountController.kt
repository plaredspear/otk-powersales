package com.otoki.powersales.common.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.dto.response.MyAccountListResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.common.service.MyAccountService
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 거래처 API Controller
 */
@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val myAccountService: MyAccountService
) {

    /**
     * 내 거래처 목록 조회
     * GET /api/v1/accounts/my
     *
     * 한 달 일정에 등록된 거래처 목록을 중복 제거하여 반환한다.
     * keyword 파라미터로 거래처명/거래처코드 검색 가능.
     */
    @GetMapping("/my")
    fun getMyAccounts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @Size(max = 100) keyword: String?
    ): ResponseEntity<ApiResponse<MyAccountListResponse>> {
        val response = myAccountService.getMyAccounts(principal.userId, keyword)
        return ResponseEntity.ok(ApiResponse.success(response, "내 거래처 목록 조회 성공"))
    }
}
