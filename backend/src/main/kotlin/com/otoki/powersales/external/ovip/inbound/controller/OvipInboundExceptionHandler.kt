package com.otoki.powersales.external.ovip.inbound.controller

import com.otoki.powersales.external.ovip.auth.controller.OvipTokenController
import com.otoki.powersales.external.ovip.inbound.dto.OvipResultWrapper
import com.otoki.powersales.platform.common.exception.BusinessException
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

// OVIP 인바운드 전용 예외 핸들러.
// assignableTypes 로 OVIP inbound 컨트롤러 (+ OvipTokenController) 에만 적용되도록 제한하며,
// 응답 포맷은 OvipResultWrapper (RESULT_CODE / RESULT_MSG) 로 통일한다.
// 기존 모바일/관리자 API 의 ApiResponse 응답과 SAP/SF 응답에는 영향이 없다.
// (SF 측 SfInboundExceptionHandler 와 동일 구조 — 응답 직렬화만 담당.
//  REQUEST_REJECTED_SCOPE audit 은 OVIP chain 의 accessDeniedHandler 에서 처리.)
@RestControllerAdvice(
    assignableTypes = [
        OvipTokenController::class,
        OvipMfeisController::class,
        OvipAccountController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class OvipInboundExceptionHandler {

    private fun <T> json(status: HttpStatus, body: OvipResultWrapper<T>): ResponseEntity<OvipResultWrapper<T>> =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<OvipResultWrapper<Nothing>> {
        val msg = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "요청 파라미터가 올바르지 않습니다"
        return json(HttpStatus.BAD_REQUEST, OvipResultWrapper(OvipResultWrapper.CODE_INVALID_PARAMETER, msg))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(): ResponseEntity<OvipResultWrapper<Nothing>> {
        return json(
            HttpStatus.BAD_REQUEST,
            OvipResultWrapper(OvipResultWrapper.CODE_INVALID_PARAMETER, "요청 본문이 올바르지 않습니다")
        )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<OvipResultWrapper<Nothing>> {
        return json(ex.httpStatus, OvipResultWrapper(ex.errorCode, ex.message))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<OvipResultWrapper<Nothing>> {
        return json(HttpStatus.FORBIDDEN, OvipResultWrapper(OvipResultWrapper.CODE_INSUFFICIENT_SCOPE, "권한 없음"))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<OvipResultWrapper<Nothing>> {
        return json(
            HttpStatus.INTERNAL_SERVER_ERROR,
            OvipResultWrapper(OvipResultWrapper.CODE_INTERNAL_ERROR, "내부 오류")
        )
    }
}
