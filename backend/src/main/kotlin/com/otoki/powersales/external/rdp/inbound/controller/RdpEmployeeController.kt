package com.otoki.powersales.external.rdp.inbound.controller

import com.otoki.powersales.external.rdp.auth.util.ClientIpResolver
import com.otoki.powersales.external.rdp.inbound.dto.EmployeeRow
import com.otoki.powersales.external.rdp.inbound.dto.RdpResultWrapper
import com.otoki.powersales.external.rdp.inbound.dto.SnapshotPageResponse
import com.otoki.powersales.external.rdp.inbound.dto.SnapshotSearchRequest
import com.otoki.powersales.external.rdp.inbound.service.RdpEmployeeQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * RDP 인바운드 — 사원(Employee) 전량 스냅샷 조회 API.
 *
 * 부하 방지 계약:
 * - PK keyset 커서(`cursor`) → offset 저하 없이 대량 순차 인출
 * - `size` 상한 clamp (rdp.inbound.employee.max-page-size)
 *
 * MFEIS 와 달리 필수 필터가 없다 — 사원 마스터는 year/month 같은 자연 파티션 축이 없고
 * 전량 인출 자체가 목적이기 때문. 노출 필드는 `employee` 테이블 전 컬럼
 * (인증 정보 `employee_info` 는 제외).
 *
 * 클라이언트는 응답 `nextCursor` 를 다음 요청 `cursor` 로 넘기며 `hasNext=false` 까지 반복 호출한다.
 */
@RestController
@RequestMapping("/api/v1/rdp/employee")
class RdpEmployeeController(
    private val queryService: RdpEmployeeQueryService
) {

    @Operation(
        summary = "사원 전량 스냅샷 조회 (keyset 페이지네이션)",
        description = "cursor(PK)/size 로 순차 인출. nextCursor 가 null 이면 마지막 페이지. 필수 필터 없음.",
        security = [SecurityRequirement(name = "Bearer")]
    )
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('SCOPE_rdp.read')")
    fun search(
        @RequestBody request: SnapshotSearchRequest,
        authentication: Authentication?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<RdpResultWrapper<SnapshotPageResponse<EmployeeRow>>> {
        val page = queryService.search(
            request = request,
            clientId = authentication?.name,
            clientIp = ClientIpResolver.resolve(httpRequest)
        )
        return ResponseEntity.ok(RdpResultWrapper.ok(page))
    }
}
