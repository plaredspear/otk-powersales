package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.HomeResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.HomeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 홈 화면 API Controller
 */
@RestController
@RequestMapping("/api/v1/home")
class HomeController(
    private val homeService: HomeService
) {

    /**
     * 홈 데이터 조회
     * GET /api/v1/home
     */
    @GetMapping
    fun getHomeData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<HomeResponse>> {
        val response = homeService.getHomeData(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
