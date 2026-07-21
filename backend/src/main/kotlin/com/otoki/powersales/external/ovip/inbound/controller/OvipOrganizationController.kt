package com.otoki.powersales.external.ovip.inbound.controller

import com.otoki.powersales.external.ovip.auth.util.ClientIpResolver
import com.otoki.powersales.external.ovip.inbound.dto.OrganizationRow
import com.otoki.powersales.external.ovip.inbound.dto.OvipResultWrapper
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotListResponse
import com.otoki.powersales.external.ovip.inbound.service.OvipOrganizationQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * OVIP 인바운드 — 조직(Organization) 전량 스냅샷 조회 API.
 *
 * 거래처/MFEIS 조회와 달리 **요청 파라미터도 페이지네이션도 없다** — 한 번의 호출로 전건이 반환된다.
 * 조직은 SAP 동기화 때 PK 가 전면 재발번되어 커서 기반 분할 인출이 스냅샷 일관성을 해치고, 지점 트리
 * 마스터라 건수도 페이지를 나눌 규모가 아니기 때문 (상세 근거는
 * [com.otoki.powersales.domain.org.organization.repository.OrganizationRepositoryCustom.findAllSnapshot] KDoc).
 *
 * 조회 계열 endpoint 규약을 맞추기 위해 GET 이 아니라 리소스 경로에 POST 를 쓴다 (MFEIS/거래처와 동일).
 * 노출 필드는 entity 전 컬럼.
 */
@RestController
@RequestMapping("/api/v1/ovip/organization")
class OvipOrganizationController(
    private val queryService: OvipOrganizationQueryService
) {

    @Operation(
        summary = "조직 전량 스냅샷 조회",
        description = "요청 파라미터 없이 전건을 한 번에 반환한다. 페이지네이션 없음.",
        security = [SecurityRequirement(name = "Bearer")]
    )
    @PostMapping
    fun search(
        authentication: Authentication?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OvipResultWrapper<SnapshotListResponse<OrganizationRow>>> {
        val result = queryService.searchAll(
            clientId = authentication?.name,
            clientIp = ClientIpResolver.resolve(httpRequest)
        )
        return ResponseEntity.ok(OvipResultWrapper.ok(result))
    }
}
