package com.otoki.internal.exception

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.ErrorDetail
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
class GlobalExceptionHandler {

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
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Any>> {
        val response = ApiResponse.error<Any>(
            code = ex.errorCode,
            message = ex.message ?: "Business logic error"
        )

        return ResponseEntity
            .status(ex.httpStatus)
            .body(response)
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

        // 로깅 (실제로는 Logger 사용)
        ex.printStackTrace()

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response)
    }
}

/**
 * 비즈니스 로직 예외
 */
open class BusinessException(
    val errorCode: String,
    override val message: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
