package com.otoki.powersales.external.sap.auth.sanity

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.auth.exception.SapSanityCheckFailedException
import com.otoki.powersales.external.sap.auth.util.ClientIpResolver
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.math.abs

@Aspect
@Component
class SapSanityCheckAspect(
    private val auditService: SapInboundAuditService
) {

    @Around("@annotation(com.otoki.powersales.external.sap.auth.sanity.SapDestructiveEndpoint)")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(SapDestructiveEndpoint::class.java)
            ?: return joinPoint.proceed()
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name

        val received = countReceived(joinPoint, signature, annotation)

        if (received <= 0) {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.REQUEST_REJECTED_SANITY,
                    clientId = clientId,
                    endpoint = endpoint,
                    httpMethod = httpMethod,
                    clientIp = clientIp,
                    receivedCount = received,
                    reason = "받은 건수 0"
                )
            )
            throw SapSanityCheckFailedException("받은 건수가 0입니다")
        }

        val previous = clientId?.let { auditService.findLatestAcceptedCount(endpoint, it) }
        if (previous != null && previous > 0) {
            val ratio = abs(received - previous).toDouble() / previous.toDouble()
            val limit = annotation.threshold.toDouble() / 100.0
            if (ratio > limit) {
                auditService.record(
                    SapInboundAudit(
                        eventType = SapInboundAuditEventType.REQUEST_REJECTED_SANITY,
                        clientId = clientId,
                        endpoint = endpoint,
                        httpMethod = httpMethod,
                        clientIp = clientIp,
                        receivedCount = received,
                        previousCount = previous,
                        reason = "변동률 ${"%.2f".format(ratio * 100)}% > ${annotation.threshold}%"
                    )
                )
                throw SapSanityCheckFailedException(
                    "받은 건수($received) 가 직전($previous) 대비 ${annotation.threshold}% 를 초과 변동했습니다"
                )
            }
        }

        val result = joinPoint.proceed()

        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                previousCount = previous
            )
        )
        return result
    }

    private fun countReceived(
        joinPoint: ProceedingJoinPoint,
        signature: MethodSignature,
        annotation: SapDestructiveEndpoint
    ): Int {
        val args = joinPoint.args
        if (annotation.countArgName.isNotBlank()) {
            val idx = signature.parameterNames.indexOf(annotation.countArgName)
            if (idx >= 0 && idx < args.size) {
                val value = args[idx]
                return when (value) {
                    is Collection<*> -> value.size
                    is Array<*> -> value.size
                    else -> 0
                }
            }
            return 0
        }
        for (arg in args) {
            when (arg) {
                is Collection<*> -> return arg.size
                is Array<*> -> return arg.size
                else -> {}
            }
        }
        return 0
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }
}
