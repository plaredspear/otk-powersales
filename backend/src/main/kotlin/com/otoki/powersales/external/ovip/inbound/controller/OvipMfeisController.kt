package com.otoki.powersales.external.ovip.inbound.controller

import com.otoki.powersales.external.ovip.auth.util.ClientIpResolver
import com.otoki.powersales.external.ovip.inbound.dto.MfeisPageResponse
import com.otoki.powersales.external.ovip.inbound.dto.MfeisSearchRequest
import com.otoki.powersales.external.ovip.inbound.dto.OvipResultWrapper
import com.otoki.powersales.external.ovip.inbound.service.OvipMfeisQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * OVIP 인바운드 — MFEIS(월별 여사원 통합일정) 전량 스냅샷 조회 API.
 *
 * 부하 방지 계약:
 * - `year` + `month` 필수 → 전체 스캔 차단
 * - PK keyset 커서(`cursor`) → offset 저하 없이 대량 순차 인출
 * - `size` 상한 clamp (ovip.inbound.mfeis.max-page-size)
 *
 * 클라이언트는 응답 `nextCursor` 를 다음 요청 `cursor` 로 넘기며 `hasNext=false` 까지 반복 호출한다.
 */
@RestController
@RequestMapping("/api/v1/ovip/mfeis")
class OvipMfeisController(
    private val queryService: OvipMfeisQueryService
) {

    @Operation(
        summary = "MFEIS 전량 스냅샷 조회 (keyset 페이지네이션)",
        description = "year+월 필수. cursor(PK)/size 로 순차 인출. nextCursor 가 null 이면 마지막 페이지.",
        security = [SecurityRequirement(name = "Bearer")]
    )
    @PostMapping("/search")
    fun search(
        @RequestBody request: MfeisSearchRequest,
        authentication: Authentication?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OvipResultWrapper<MfeisPageResponse>> {
        val page = queryService.search(
            request = request,
            clientId = authentication?.name,
            clientIp = ClientIpResolver.resolve(httpRequest)
        )
        return ResponseEntity.ok(OvipResultWrapper.ok(page))
    }
}
