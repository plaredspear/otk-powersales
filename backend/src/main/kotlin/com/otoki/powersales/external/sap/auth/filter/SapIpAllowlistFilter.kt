package com.otoki.powersales.external.sap.auth.filter

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.auth.util.ClientIpResolver
import com.otoki.powersales.external.sap.auth.util.IpAllowlistMatcher
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

// /api/v1/sap/** 진입 IP 검증. 허용 CIDR 가 비어 있으면 검증을 건너뛴다 (ALB 단에 위임).
class SapIpAllowlistFilter(
    private val ipMatcher: IpAllowlistMatcher,
    private val auditService: SapInboundAuditService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!ipMatcher.isEnabled) {
            filterChain.doFilter(request, response)
            return
        }
        val clientIp = ClientIpResolver.resolve(request)
        if (!ipMatcher.matches(clientIp)) {
            runCatching {
                auditService.record(
                    SapInboundAudit(
                        eventType = SapInboundAuditEventType.REQUEST_REJECTED_IP,
                        clientId = null,
                        endpoint = request.requestURI,
                        httpMethod = request.method,
                        clientIp = clientIp,
                        reason = "허용되지 않은 IP"
                    )
                )
            }
            SapAuthErrorWriter.write(response, objectMapper, 403, "IP_NOT_ALLOWED", "허용되지 않은 IP")
            return
        }
        filterChain.doFilter(request, response)
    }
}
