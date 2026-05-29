package com.otoki.powersales.sf.inbound.controller

import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sf.inbound.dto.SfResultWrapper
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// SF 인바운드 전용 예외 핸들러 (Spec #775).
// assignableTypes 로 SF inbound 컨트롤러 (+ SfTokenController) 에만 적용되도록 제한하며,
// 응답 포맷은 SfResultWrapper (RESULT_CODE / RESULT_MSG) 로 통일한다.
// 기존 모바일/관리자 API 의 ApiResponse 응답과 SAP /api/v1/sap/** 응답에는 영향이 없다.
//
// SAP 측 SapInboundExceptionHandler 가 audit 적재까지 책임 가졌던 것과 달리, 본 핸들러는 응답
// 직렬화만 담당하여 의존성을 단순화한다 — admin/mobile 측 WebMvcTest 슬라이스가
// SfInboundAuditService 를 추가로 mock 하지 않아도 된다. REQUEST_REJECTED_SCOPE audit 적재는
// SF chain 의 accessDeniedHandler (SfAuthSecurityConfig) 에서 처리.
@RestControllerAdvice(
    assignableTypes = [
        com.otoki.powersales.sf.auth.controller.SfTokenController::class,
        com.otoki.powersales.sf.inbound.health.SfInboundHealthController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SfInboundExceptionHandler {

    private fun <T> json(status: HttpStatus, body: SfResultWrapper<T>): ResponseEntity<SfResultWrapper<T>> =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<SfResultWrapper<Nothing>> {
        val msg = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "요청 페이로드가 올바르지 않습니다"
        return json(HttpStatus.BAD_REQUEST, SfResultWrapper(SfResultWrapper.CODE_INVALID_PAYLOAD, msg))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(): ResponseEntity<SfResultWrapper<Nothing>> {
        return json(HttpStatus.BAD_REQUEST, SfResultWrapper(SfResultWrapper.CODE_INVALID_PAYLOAD, "요청 본문이 올바르지 않습니다"))
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<SfResultWrapper<Nothing>> {
        return json(ex.httpStatus, SfResultWrapper(ex.errorCode, ex.message))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<SfResultWrapper<Nothing>> {
        return json(HttpStatus.FORBIDDEN, SfResultWrapper(SfResultWrapper.CODE_INSUFFICIENT_SCOPE, "권한 없음"))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<SfResultWrapper<Nothing>> {
        return json(HttpStatus.INTERNAL_SERVER_ERROR, SfResultWrapper(SfResultWrapper.CODE_INTERNAL_ERROR, "내부 오류"))
    }
}
