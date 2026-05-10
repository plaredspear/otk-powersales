package com.otoki.powersales.sap.auth.audit

import com.otoki.powersales.sap.auth.exception.SapSanityCheckFailedException
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.SapInboundChunkedResult
import com.otoki.powersales.sap.inbound.dto.SapInboundUpsertResult
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
 * `@SapInboundAccepted` 부착 메서드의 `REQUEST_ACCEPTED` audit 기록을 공통화한 around advice. (Spec #639)
 *
 * 12건 SAP 인바운드 어댑터(`SapClientMasterService` 외 11건) 가 각자 보유하던 ~17줄 `recordAccepted`
 * 보일러플레이트(servlet attribute → request 추출 / endpoint / clientIp / clientId / auditService.record)
 * 를 본 advice 로 통합한다.
 *
 * 동작 시나리오:
 * - happy path: 메서드 정상 return + 결과가 [SapInboundUpsertResult] 또는 [SapInboundChunkedResult]
 *   → `success` / `failure` (/ `chunks`) placeholder 치환된 reason 으로 audit 1회 기록
 * - throw path: 메서드 또는 inner aspect 가 throw → `success=0 failure=<received>` audit + rethrow
 *   (#634 정책을 12건 전체로 통일 — `legacy-deviation.md` §외부 연동 `sap-inbound-audit-on-throw` 참조)
 * - sanity reject: [SapSanityCheckFailedException] 은 [SapSanityCheckAspect] 가 별도 audit 을 이미
 *   기록하므로 본 advice 는 audit 없이 즉시 rethrow
 *
 * Aspect 가 `@Transactional` 외부에 위치하므로 도메인 트랜잭션 commit 후 audit 이 기록된다.
 */
@Aspect
@Component
class SapInboundAuditAspect(
    private val auditService: SapInboundAuditService
) {

    @Around("@annotation(com.otoki.powersales.sap.auth.audit.SapInboundAccepted)")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(SapInboundAccepted::class.java)
            ?: return joinPoint.proceed()

        val received = countReceived(joinPoint, signature, annotation.countArgName)

        val result = try {
            joinPoint.proceed()
        } catch (ex: SapSanityCheckFailedException) {
            throw ex
        } catch (ex: Throwable) {
            recordAudit(annotation.reasonTemplate, received, success = 0, failure = received, chunks = 0)
            throw ex
        }

        val success: Int
        val failure: Int
        val chunks: Int
        when (result) {
            is SapInboundChunkedResult -> {
                success = result.successCount
                failure = result.failureCount
                chunks = result.chunkCount
            }
            is SapInboundUpsertResult -> {
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
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
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
