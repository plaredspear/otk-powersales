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
import com.fasterxml.jackson.databind.exc.MismatchedInputException
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
        // 값 없이 실패 필드 경로만 기록 — "reqItemList 가 null/누락(키 표기 불일치 포함)" 을 사후 진단.
        val failedFields = ex.bindingResult.fieldErrors
            .mapNotNull { it.field.takeIf { f -> f.isNotBlank() } }
            .distinct()
            .joinToString(", ")
        recordPayloadRejection(reason = "검증 실패 field=[$failedFields] msg=$msg")
        return json(HttpStatus.BAD_REQUEST, SapResultWrapper(SapResultWrapper.Companion.CODE_INVALID_PAYLOAD, msg))
    }

    // 역직렬화 실패(깨진 JSON / 타입 불일치 등) → 400 + CODE_INVALID_PAYLOAD.
    // 미정의(unknown) 필드는 여기로 오지 않는다 — 전역 lenient 정책상 무시되고 처리가 계속된다.
    // (정책 단일 출처: JacksonConfig 의 "미정의 JSON 필드 정책 — lenient 유지" 주석. 레거시
    //  JSON.deserializeStrict 의 unknown-field 전체 실패와는 의도적으로 다르게 채택함.)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<SapResultWrapper<Nothing>> {
        recordPayloadRejection(reason = "역직렬화 실패 ${describeUnreadable(ex)}")
        return json(HttpStatus.BAD_REQUEST,
            SapResultWrapper(SapResultWrapper.Companion.CODE_INVALID_PAYLOAD, "요청 본문이 올바르지 않습니다")
        )
    }

    /**
     * 역직렬화 실패를 업무 데이터(값) 없이 구조 메타로만 요약한다.
     * MismatchedInputException 이면 대상 타입 + JSON path(필드 경로) 를 뽑아 "어느 필드가 배열/타입이 안 맞았는가" 를 남기고,
     * 그 외(깨진 JSON 등)는 예외 클래스명만 남긴다. 예외 message 는 원문 JSON 조각을 포함할 수 있어 사용하지 않는다.
     */
    private fun describeUnreadable(ex: HttpMessageNotReadableException): String {
        val cause = ex.mostSpecificCause
        if (cause is MismatchedInputException) {
            val path = cause.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }.ifBlank { "(root)" }
            val target = cause.targetType?.simpleName ?: "?"
            return "type=MismatchedInput path=$path target=$target"
        }
        return "type=${cause::class.simpleName}"
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

    // AccessDeniedException 은 catch-all 보다 먼저 잡아 403 + INSUFFICIENT_SCOPE 로 매핑한다.
    // 현재 inbound 컨트롤러는 scope 기반 @PreAuthorize 를 사용하지 않으나 (폐쇄망 + IP/client 인증으로
    // 신뢰 경계 확보), Spring Security 가 다른 경로로 AccessDeniedException 을 던질 경우의 안전망으로 유지한다.
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

    // 400(검증/파싱 실패) 을 REQUEST_REJECTED_PAYLOAD 로 기록. reason 에는 값 없이 구조 메타만 담는다.
    // audit 적재 실패가 400 응답을 500 으로 뒤집지 않도록 recordScopeRejection 과 동일하게 격리한다.
    private fun recordPayloadRejection(reason: String) {
        val request = currentRequest() ?: return
        val auth = SecurityContextHolder.getContext().authentication
        runCatching {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.REQUEST_REJECTED_PAYLOAD,
                    clientId = auth?.name,
                    endpoint = request.requestURI,
                    httpMethod = request.method,
                    clientIp = ClientIpResolver.resolve(request),
                    reason = reason.take(1000)
                )
            )
        }
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
