package com.otoki.powersales.domain.activity.suggestion.controller

import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionCreateRequest
import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionDraftRequest
import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionUpdateRequest
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionCreateResponse
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionDraftResponse
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionListItem
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionResponse
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.service.SuggestionDraftService
import com.otoki.powersales.domain.activity.suggestion.service.SuggestionService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

/**
 * 제안하기 API Controller — Spec #664 P2-B.
 *
 * ## 레거시 매핑
 * - SF Apex: `IF_REST_MOBILE_ProposalRegist.cls#doPost`, `IF_REST_MOBILE_LogisticsClaimSearch.cls#doPost` (조회는 #667 별 스펙)
 */
@RestController
@RequestMapping("/api/v1/mobile/suggestions")
class SuggestionController(
    private val suggestionService: SuggestionService,
    private val suggestionDraftService: SuggestionDraftService
) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestPart("request") request: SuggestionCreateRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<SuggestionCreateResponse>> {
        val result = suggestionService.create(
            employeeId = principal.userId,
            request = request,
            photos = photos
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "제안이 등록되었습니다"))
    }

    /**
     * 제안 임시저장 (upsert). 검증 없이 사원 1건의 draft 를 갱신한다.
     * 레거시 FieldTalkController.tempSuggestProc 대응. 사진 part 전달 시 전체 교체(최대 2장).
     */
    @PostMapping("/draft", consumes = ["multipart/form-data"])
    fun saveDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestPart("request") request: SuggestionDraftRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<SuggestionDraftResponse>> {
        val result = suggestionDraftService.saveDraft(principal.userId, request, photos)
        return ResponseEntity.ok(ApiResponse.success(result, "임시저장되었습니다"))
    }

    /** 제안 임시저장 조회. 없으면 data=null. */
    @GetMapping("/draft")
    fun getDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<SuggestionDraftResponse?>> {
        val result = suggestionDraftService.getDraft(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /** 제안 임시저장 폐기. */
    @DeleteMapping("/draft")
    fun deleteDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Unit>> {
        suggestionDraftService.deleteDraft(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "임시저장이 삭제되었습니다"))
    }

    @GetMapping("/{suggestionId}")
    fun getDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long
    ): ResponseEntity<ApiResponse<SuggestionResponse>> {
        val response = suggestionService.getDetail(suggestionId, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 본인 제안/물류클레임 목록. category 미지정 시 전체, 지정 시 해당 분류만.
     * 레거시 `logisticsclaimlist`(물류클레임 전용 조회) 진입은 category=LOGISTICS_CLAIM 으로 호출.
     *
     * 레거시 검색조건 정합: [accountId] 거래처(SAPAccountCode), [startDate]~[endDate] 등록일 범위.
     */
    @GetMapping
    fun listMine(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) category: SuggestionCategory?,
        @RequestParam(required = false) accountId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<ApiResponse<Page<SuggestionListItem>>> {
        val response = suggestionService.listMine(
            employeeId = principal.userId,
            page = page,
            size = size,
            category = category,
            accountId = accountId,
            startDate = startDate,
            endDate = endDate
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{suggestionId}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long,
        @Valid @RequestBody request: SuggestionUpdateRequest
    ): ResponseEntity<ApiResponse<SuggestionResponse>> {
        val response = suggestionService.update(suggestionId, principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "제안이 수정되었습니다"))
    }

    @DeleteMapping("/{suggestionId}")
    fun softDelete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        suggestionService.softDelete(suggestionId, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "제안이 삭제되었습니다"))
    }

    /**
     * UC-06: 제안 첨부 사진 단건 삭제 (Spec #828). 본인 row 한정, 상태 무관 허용.
     */
    @DeleteMapping("/{suggestionId}/photos/{photoId}")
    fun deletePhoto(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long,
        @PathVariable photoId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        suggestionService.deletePhoto(principal.userId, suggestionId, photoId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "사진이 삭제되었습니다"))
    }
}
