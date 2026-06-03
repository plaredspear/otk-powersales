package com.otoki.powersales.savedsearch.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.savedsearch.dto.request.SavedSearchCreateRequest
import com.otoki.powersales.savedsearch.dto.request.SavedSearchUpdateRequest
import com.otoki.powersales.savedsearch.dto.response.SavedSearchResponse
import com.otoki.powersales.savedsearch.service.SavedSearchService
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 저장된 검색 (SavedSearch) API (Spec #852).
 *
 * 공용(SHARED) 생성/수정/삭제 권한은 `saved_search` EDIT 로 가드하되, PRIVATE/SHARED 가 한 엔드포인트에
 * 혼재하므로 컨트롤러 어노테이션 가드 대신 서비스 내부에서 호출자 권한 snapshot 으로 분기 검증한다.
 */
@RestController
@RequestMapping("/api/v1/admin/saved-searches")
@Validated
class SavedSearchController(
    private val savedSearchService: SavedSearchService,
) {

    @GetMapping
    fun list(
        @RequestParam @Size(min = 1, max = 50) resourceKey: String,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<SavedSearchResponse>>> {
        val response = savedSearchService.list(
            resourceKey = resourceKey,
            employeeId = principal.requireEmployeeId(),
            permissions = principal.permissions,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: SavedSearchCreateRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<SavedSearchResponse>> {
        val response = savedSearchService.create(
            request = request,
            employeeId = principal.requireEmployeeId(),
            permissions = principal.permissions,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: SavedSearchUpdateRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<SavedSearchResponse>> {
        val response = savedSearchService.update(
            id = id,
            request = request,
            employeeId = principal.requireEmployeeId(),
            permissions = principal.permissions,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<Void> {
        savedSearchService.delete(
            id = id,
            employeeId = principal.requireEmployeeId(),
            permissions = principal.permissions,
        )
        return ResponseEntity.noContent().build()
    }
}
