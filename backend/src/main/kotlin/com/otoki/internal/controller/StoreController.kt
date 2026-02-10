package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.MyStoreListResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.MyStoreService
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 거래처 API Controller
 */
@RestController
@RequestMapping("/api/v1/stores")
class StoreController(
    private val myStoreService: MyStoreService
) {

    /**
     * 내 거래처 목록 조회
     * GET /api/v1/stores/my
     *
     * 한 달 일정에 등록된 거래처 목록을 중복 제거하여 반환한다.
     * keyword 파라미터로 거래처명/거래처코드 검색 가능.
     */
    @GetMapping("/my")
    fun getMyStores(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @Size(max = 100) keyword: String?
    ): ResponseEntity<ApiResponse<MyStoreListResponse>> {
        val response = myStoreService.getMyStores(principal.userId, keyword)
        return ResponseEntity.ok(ApiResponse.success(response, "내 거래처 목록 조회 성공"))
    }
}
