package com.otoki.powersales.platform.common.exception

import com.otoki.powersales.platform.auth.exception.NewPasswordPolicyViolationException
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.ErrorDetail
import com.otoki.powersales.domain.activity.promotion.exception.BatchValidationException
import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.slf4j.LoggerFactory

/**
 * 전역 예외 처리 핸들러 (REST API 전용).
 *
 * `annotations = [RestController::class]` 제한을 두지 않는다 — 그 제한은 컨트롤러 매칭이
 * 끝난 후에만 동작하므로, HTTP 메서드/미디어 타입 불일치처럼 매칭 단계 자체에서 발생하는
 * 표준 예외를 가로챌 수 없다. 본 backend 는 view 반환용 `@Controller` 가 없고 모두
 * `@RestController` 라 제한 제거 시 부작용 없음.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val errors = ex.bindingResult.allErrors
            .mapNotNull { error ->
                val fieldName = (error as? FieldError)?.field ?: "unknown"
                val errorMessage = error.defaultMessage ?: "Validation failed"
                "$fieldName: $errorMessage"
            }
            .joinToString(", ")

        val response = ApiResponse.error<Any>(
            code = "INVALID_PARAMETER",
            message = errors
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * 요청 본문 파싱 오류 예외 처리 (필수 필드 누락, JSON 형식 오류 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "INVALID_PARAMETER",
            message = "요청 본문이 올바르지 않습니다"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * 필수 요청 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "INVALID_PARAMETER",
            message = "필수 파라미터 '${ex.parameterName}'이(가) 누락되었습니다"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * Path variable / 요청 파라미터 타입 변환 실패 처리.
     * 예: `GET /api/v1/admin/display-work-schedule/abc` 처럼 Long path variable 에 비숫자 값이 들어온 경우.
     * 핸들러가 없으면 500 이 되므로 400 으로 명시 처리한다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "INVALID_PARAMETER",
            message = "파라미터 '${ex.name}'의 값이 올바르지 않습니다"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * 잘못된 인자 예외 처리 (파라미터 검증 실패)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "INVALID_PARAMETER",
            message = ex.message ?: "잘못된 요청입니다"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }

    /**
     * 인증 실패 예외 처리
     */
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(
        ex: BadCredentialsException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "INVALID_CREDENTIALS",
            message = "사번 또는 비밀번호가 올바르지 않습니다"
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(response)
    }

    /**
     * 권한 없음 예외 처리
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "ACCESS_DENIED",
            message = "접근 권한이 없습니다"
        )

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(response)
    }

    /**
     * 일괄 수정 검증 실패 예외 처리
     */
    @ExceptionHandler(BatchValidationException::class)
    fun handleBatchValidationException(
        ex: BatchValidationException,
        request: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "errorCode" to ex.errorCode,
            "message" to (ex.message ?: ""),
            "detail" to mapOf("errors" to ex.errors)
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(body)
    }

    /**
     * 새 비밀번호 정책 위반 (Spec #584) — `error.details.violations` 에 위반 규칙 코드 배열을 포함한다.
     */
    @ExceptionHandler(NewPasswordPolicyViolationException::class)
    fun handleNewPasswordPolicyViolationException(
        ex: NewPasswordPolicyViolationException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        val errorDetail = ErrorDetail(
            code = ex.errorCode,
            message = ex.message ?: "",
            details = mapOf("violations" to ex.violations)
        )
        return ResponseEntity
            .status(ex.httpStatus)
            .body(ApiResponse.error<Any?>(errorDetail))
    }

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        // 5xx(서버 결함: STORAGE_WRITE_FAILED 등)는 스택트레이스까지 error 로 남겨 추적 가능하게 한다.
        // 4xx(클라이언트 요청 오류)는 정상 흐름의 일부라 스택 없이 warn 요약만 남긴다(로그 노이즈 회피).
        if (ex.httpStatus.is5xxServerError) {
            log.error("BusinessException {} ({}): {}", ex.errorCode, ex.httpStatus, ex.message, ex)
        } else {
            log.warn("BusinessException {} ({}): {}", ex.errorCode, ex.httpStatus, ex.message)
        }

        val response = ApiResponse.error<Any>(
            code = ex.errorCode,
            message = ex.message ?: "Business logic error"
        )

        return ResponseEntity
            .status(ex.httpStatus)
            .body(response)
    }

    /**
     * 낙관적 락 충돌 처리 — @Version 엔티티를 두 요청이 거의 동시에 수정해, service 의 선제 version 검사를
     * 둘 다 통과한 뒤 flush 시점에 JPA 가 버전 불일치를 감지한 경우(진짜 race). 나중 커밋을 409 로 거부한다.
     * (대부분의 동시 편집은 service 의 선제 검사에서 도메인 예외로 먼저 걸러지고, 이 경로는 그 사이 좁은
     *  타이밍 창을 메우는 안전망이다.) 정상 흐름의 일부라 스택 없이 warn 요약만 남긴다.
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLockConflict(
        ex: org.springframework.orm.ObjectOptimisticLockingFailureException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        log.warn("Optimistic lock conflict: {}", ex.message)
        val response = ApiResponse.error<Any>(
            code = "VERSION_CONFLICT",
            message = "다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 저장해주세요"
        )
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response)
    }

    // HTTP 메서드 불일치는 컨트롤러 매칭 전에 발생해 RestControllerAdvice assignableTypes 로 잡히지 않으므로 글로벌에서 처리.
    // SAP 인바운드 path 는 SapResultWrapper 형식, 그 외는 ApiResponse 형식.
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: WebRequest
    ): ResponseEntity<Any> {
        val supported = ex.supportedMethods?.joinToString(", ") ?: "POST"
        val message = "지원하지 않는 HTTP 메서드. 허용: $supported"
        val body: Any = if (isSapInboundPath(request)) {
            SapResultWrapper<Nothing>(SapResultWrapper.CODE_METHOD_NOT_ALLOWED, message)
        } else {
            ApiResponse.error<Any>(code = "METHOD_NOT_ALLOWED", message = message)
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
    }

    private fun isSapInboundPath(request: WebRequest): Boolean {
        val servletRequest = (request as? ServletWebRequest)?.request ?: return false
        return servletRequest.requestURI?.startsWith("/api/v1/sap/") == true
    }

    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = "INTERNAL_SERVER_ERROR",
            message = "서버 내부 오류가 발생했습니다"
        )

        // 예상치 못한 서버 오류 — 스택트레이스까지 error 로 남긴다(printStackTrace 는 로거를 우회해 집계/포맷 누락).
        log.error("Unhandled exception: {}", ex.message, ex)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response)
    }
}
