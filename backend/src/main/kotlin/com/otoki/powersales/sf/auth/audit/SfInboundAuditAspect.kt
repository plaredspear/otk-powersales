package com.otoki.powersales.sf.auth.audit

import com.otoki.powersales.sf.auth.util.ClientIpResolver
import com.otoki.powersales.sf.inbound.dto.SfInboundChunkedResult
import com.otoki.powersales.sf.inbound.dto.SfInboundUpsertResult
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * `@SfInboundAccepted` 부착 메서드의 `REQUEST_ACCEPTED` audit 기록을 공통화한 around advice.
 *
 * SAP 측 [com.otoki.powersales.sap.auth.audit.SapInboundAuditAspect] 와 격리된 별도 인스턴스 —
 * SF 인바운드만 처리한다 (Spec #774).
 *
 * 동작 시나리오:
 * - happy path: 메서드 정상 return + 결과가 [SfInboundUpsertResult] 또는 [SfInboundChunkedResult]
 *   → `success` / `failure` (/ `chunks`) placeholder 치환된 reason 으로 audit 1회 기록
 * - throw path: 메서드 또는 inner aspect 가 throw → `success=0 failure=<received>` audit + rethrow
 *
 * Aspect 가 `@Transactional` 외부에 위치하므로 도메인 트랜잭션 commit 후 audit 이 기록된다.
 */
@Aspect
@Component
class SfInboundAuditAspect(
    private val auditService: SfInboundAuditService
) {

    @Around("@annotation(com.otoki.powersales.sf.auth.audit.SfInboundAccepted)")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(SfInboundAccepted::class.java)
            ?: return joinPoint.proceed()

        val received = countReceived(joinPoint, signature, annotation.countArgName)

        val result = try {
            joinPoint.proceed()
        } catch (ex: Throwable) {
            recordAudit(annotation.reasonTemplate, received, success = 0, failure = received, chunks = 0)
            throw ex
        }

        val success: Int
        val failure: Int
        val chunks: Int
        when (result) {
            is SfInboundChunkedResult -> {
                success = result.successCount
                failure = result.failureCount
                chunks = result.chunkCount
            }
            is SfInboundUpsertResult -> {
                success = result.successCount
                failure = result.failureCount
                chunks = 0
            }
            else -> {
                success = 0
                failure = 0
                chunks = 0
            }
        }
        recordAudit(annotation.reasonTemplate, received, success, failure, chunks)
        return result
    }

    private fun recordAudit(
        reasonTemplate: String,
        received: Int,
        success: Int,
        failure: Int,
        chunks: Int
    ) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        val reason = reasonTemplate
            .replace("{success}", success.toString())
            .replace("{failure}", failure.toString())
            .replace("{chunks}", chunks.toString())
        auditService.record(
            SfInboundAudit(
                eventType = SfInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = reason
            )
        )
    }

    private fun countReceived(
        joinPoint: ProceedingJoinPoint,
        signature: MethodSignature,
        countArgName: String
    ): Int {
        val args = joinPoint.args
        if (countArgName.isNotBlank()) {
            val idx = signature.parameterNames.indexOf(countArgName)
            if (idx in args.indices) {
                return when (val value = args[idx]) {
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
