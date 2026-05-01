package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 인바운드 전용 예외 핸들러. (Spec #557, #558)
 *
 * `assignableTypes` 로 SAP 인바운드 컨트롤러에만 적용되도록 제한하며, 응답 포맷은
 * [SapResultWrapper] (`RESULT_CODE` / `RESULT_MSG`) 로 통일한다.
 * 기존 모바일/관리자 API 의 [com.otoki.powersales.common.dto.ApiResponse] 응답에는 영향이 없다.
 */
@RestControllerAdvice(
    assignableTypes = [
        SapAccountMasterController::class,
        SapEmployeeMasterController::class,
        SapProductMasterController::class,
        SapSalesHistoryController::class,
        SapErpOrderController::class,
        SapAppointmentController::class,
        SapAttendInfoController::class,
        com.otoki.powersales.claim.controller.SapClaimStatusController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SapInboundExceptionHandler(
    private val auditService: SapInboundAuditService
) {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<SapResultWrapper<Nothing>> {
        val msg = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "요청 페이로드가 올바르지 않습니다"
        return ResponseEntity.badRequest()
            .body(SapResultWrapper(SapResultWrapper.CODE_INVALID_PAYLOAD, msg))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(): ResponseEntity<SapResultWrapper<Nothing>> {
        return ResponseEntity.badRequest()
            .body(SapResultWrapper(SapResultWrapper.CODE_INVALID_PAYLOAD, "요청 본문이 올바르지 않습니다"))
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<SapResultWrapper<Nothing>> {
        return ResponseEntity.status(ex.httpStatus)
            .body(SapResultWrapper(ex.errorCode, ex.message))
    }

    // method-level @PreAuthorize 가 던진 AccessDeniedException 은 catch-all 보다 먼저 잡아 403 + INSUFFICIENT_SCOPE 로 매핑.
    // SecurityFilterChain 의 accessDeniedHandler 와 의미를 일치시키고, REQUEST_REJECTED_SCOPE audit 도 함께 기록.
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<SapResultWrapper<Nothing>> {
        recordScopeRejection(ex)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(SapResultWrapper(SapResultWrapper.CODE_INSUFFICIENT_SCOPE, "권한 없음"))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<SapResultWrapper<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(SapResultWrapper(SapResultWrapper.CODE_INTERNAL_ERROR, "내부 오류"))
    }

    // audit 적재 실패가 메인 응답(403) 을 500 으로 뒤집지 않도록 격리.
    // SapBearerTokenFilter / SapIpAllowlistFilter 와 동일 패턴.
    private fun recordScopeRejection(ex: AccessDeniedException) {
        val request = currentRequest() ?: return
        val auth = SecurityContextHolder.getContext().authentication
        val grantedScopes = auth?.authorities
            ?.mapNotNull { it.authority?.removePrefix("SCOPE_")?.takeIf { s -> s.isNotBlank() } }
            ?.joinToString(" ")
            .orEmpty()
        runCatching {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.REQUEST_REJECTED_SCOPE,
                    clientId = auth?.name,
                    endpoint = request.requestURI,
                    httpMethod = request.method,
                    clientIp = ClientIpResolver.resolve(request),
                    scope = grantedScopes,
                    reason = ex.message?.take(1000) ?: "토큰 scope 가 엔드포인트 요구 권한과 불일치"
                )
            )
        }
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }
}
