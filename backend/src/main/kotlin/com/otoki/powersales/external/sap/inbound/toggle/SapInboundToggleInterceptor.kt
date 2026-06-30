package com.otoki.powersales.external.sap.inbound.toggle

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.auth.util.ClientIpResolver
import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

/**
 * SAP 인바운드 처리 활성/비활성 게이트 인터셉터.
 *
 * endpoint 가 [SapInboundToggleStore] 에서 **비활성** 인 경우:
 * - 컨트롤러/서비스(적재 처리)로 진입시키지 않는다 (`preHandle` 에서 false 반환).
 * - 정상(200) + [SapResultWrapper.ok] (RESULT_CODE=200, RESULT_MSG="OK") 응답을 직접 기록한다.
 *   → SAP 측에는 성공으로 보이지만 실제 데이터 적재는 일어나지 않는다 (요구사항).
 * - audit 에 [SapInboundAuditEventType.REQUEST_SKIPPED] 1행을 남겨 "언제 무엇을 건너뛰었는지" 추적 가능.
 *
 * Spring Security 인증 필터 통과 후(= SecurityContext 에 clientId 설정 후) DispatcherServlet
 * 단계에서 실행되므로, 정상 audit aspect 와 동일한 메타데이터(clientId/clientIp)를 확보할 수 있다.
 *
 * 활성(기본)인 경우 아무 동작 없이 통과시켜 기존 흐름을 그대로 유지한다.
 *
 * 빈이 아니라 [SapInboundToggleWebConfig] 에서 직접 인스턴스화한다 — `HandlerInterceptor` 를
 * `@Component` 로 두면 `@WebMvcTest` 슬라이스가 이를 자동 스캔해 모든 MVC 슬라이스 테스트가
 * 본 인터셉터의 의존성을 요구하게 되기 때문이다.
 */
class SapInboundToggleInterceptor(
    private val toggleStore: SapInboundToggleStore,
    private val auditService: SapInboundAuditService,
    private val objectMapper: ObjectMapper,
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(SapInboundToggleInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val endpointPath = request.requestURI
        if (toggleStore.isEnabled(endpointPath)) {
            return true
        }

        log.info("[SAP_INBOUND_TOGGLE] 비활성 endpoint 요청 수신 — 처리 생략 + 정상 응답. endpoint={}", endpointPath)
        recordSkippedAudit(request, endpointPath)
        writeOkResponse(response)
        return false
    }

    private fun recordSkippedAudit(request: HttpServletRequest, endpointPath: String) {
        try {
            val clientId = SecurityContextHolder.getContext().authentication?.name
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.REQUEST_SKIPPED,
                    clientId = clientId,
                    endpoint = endpointPath,
                    httpMethod = request.method,
                    clientIp = ClientIpResolver.resolve(request),
                    reason = "비활성 상태 — 적재 처리 생략 후 정상 응답",
                )
            )
        } catch (ex: Exception) {
            // audit 실패가 정상 응답을 막아서는 안 된다 (best-effort).
            log.warn("[SAP_INBOUND_TOGGLE] SKIPPED audit 기록 실패. endpoint={} cause={}", endpointPath, ex.message)
        }
    }

    private fun writeOkResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_OK
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(SapResultWrapper.ok<Any>()))
    }
}
