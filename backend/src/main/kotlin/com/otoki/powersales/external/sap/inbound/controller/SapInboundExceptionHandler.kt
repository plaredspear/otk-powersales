package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.auth.controller.SapTokenController
import com.otoki.powersales.external.sap.auth.util.ClientIpResolver
import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
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
 * 기존 모바일/관리자 API 의 [com.otoki.powersales.platform.common.dto.ApiResponse] 응답에는 영향이 없다.
 */
@RestControllerAdvice(
    assignableTypes = [
        SapOrganizationMasterController::class,
        SapAccountMasterController::class,
        SapEmployeeMasterController::class,
        SapProductMasterController::class,
        SapErpOrderController::class,
        SapAppointmentController::class,
        SapAttendInfoController::class,
        SapTokenController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SapInboundExceptionHandler(
    private val auditService: SapInboundAuditService
) {

    // SAP 측이 잘못된 Accept 헤더(application/xml 등) 를 보내도 응답은 항상 application/json 으로 강제.
    // contentType 명시 없이 ResponseEntity 만 반환하면 응답 직렬화 단계에서 Accept 와 충돌 → 500 으로 떨어짐.
    private fun <T> json(status: HttpStatus, body: SapResultWrapper<T>): ResponseEntity<SapResultWrapper<T>> =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<SapResultWrapper<Nothing>> {
        val msg = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "요청 페이로드가 올바르지 않습니다"
        return json(HttpStatus.BAD_REQUEST, SapResultWrapper(SapResultWrapper.Companion.CODE_INVALID_PAYLOAD, msg))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(): ResponseEntity<SapResultWrapper<Nothing>> {
        return json(HttpStatus.BAD_REQUEST,
            SapResultWrapper(SapResultWrapper.Companion.CODE_INVALID_PAYLOAD, "요청 본문이 올바르지 않습니다")
        )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<SapResultWrapper<Nothing>> {
        return json(ex.httpStatus, SapResultWrapper(ex.errorCode, ex.message))
    }

    // Spring 표준 예외 — catch-all 보다 먼저 잡아 정확한 4xx 로 매핑.
    // 기존엔 `@ExceptionHandler(Exception::class)` 가 가로채 500 INTERNAL_ERROR 로 응답되던 것을 회복.
    // (HttpRequestMethodNotSupportedException 은 컨트롤러 매칭 전에 발생해 여기서 잡히지 않으므로 GlobalExceptionHandler 가 처리)
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException): ResponseEntity<SapResultWrapper<Nothing>> {
        val supported = ex.supportedMediaTypes.joinToString(", ").ifBlank { "application/json" }
        return json(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            SapResultWrapper(
                SapResultWrapper.Companion.CODE_UNSUPPORTED_MEDIA_TYPE,
                "지원하지 않는 Content-Type. 허용: $supported"
            )
        )
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleNotAcceptable(): ResponseEntity<SapResultWrapper<Nothing>> {
        return json(
            HttpStatus.NOT_ACCEPTABLE,
            SapResultWrapper(SapResultWrapper.Companion.CODE_NOT_ACCEPTABLE, "Accept 헤더가 application/json 을 포함해야 합니다")
        )
    }

    // method-level @PreAuthorize 가 던진 AccessDeniedException 은 catch-all 보다 먼저 잡아 403 + INSUFFICIENT_SCOPE 로 매핑.
    // SecurityFilterChain 의 accessDeniedHandler 와 의미를 일치시키고, REQUEST_REJECTED_SCOPE audit 도 함께 기록.
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<SapResultWrapper<Nothing>> {
        recordScopeRejection(ex)
        return json(HttpStatus.FORBIDDEN, SapResultWrapper(SapResultWrapper.Companion.CODE_INSUFFICIENT_SCOPE, "권한 없음"))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<SapResultWrapper<Nothing>> {
        return json(HttpStatus.INTERNAL_SERVER_ERROR,
            SapResultWrapper(SapResultWrapper.Companion.CODE_INTERNAL_ERROR, "내부 오류")
        )
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
