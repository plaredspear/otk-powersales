package com.otoki.powersales.external.rdp.inbound.controller

import com.otoki.powersales.external.rdp.auth.controller.RdpTokenController
import com.otoki.powersales.external.rdp.inbound.dto.RdpResultWrapper
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

// RDP 인바운드 전용 예외 핸들러.
// assignableTypes 로 RDP inbound 컨트롤러 (+ RdpTokenController) 에만 적용되도록 제한하며,
// 응답 포맷은 RdpResultWrapper (RESULT_CODE / RESULT_MSG) 로 통일한다.
// 기존 모바일/관리자 API 의 ApiResponse 응답과 SAP/SF 응답에는 영향이 없다.
// (SF 측 SfInboundExceptionHandler 와 동일 구조 — 응답 직렬화만 담당.
//  REQUEST_REJECTED_SCOPE audit 은 RDP chain 의 accessDeniedHandler 에서 처리.)
@RestControllerAdvice(
    assignableTypes = [
        RdpTokenController::class,
        RdpMfeisController::class,
        RdpAccountController::class,
        RdpEmployeeController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class RdpInboundExceptionHandler {

    private fun <T> json(status: HttpStatus, body: RdpResultWrapper<T>): ResponseEntity<RdpResultWrapper<T>> =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<RdpResultWrapper<Nothing>> {
        val msg = ex.bindingResult.allErrors.firstOrNull()?.defaultMessage ?: "요청 파라미터가 올바르지 않습니다"
        return json(HttpStatus.BAD_REQUEST, RdpResultWrapper(RdpResultWrapper.CODE_INVALID_PARAMETER, msg))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(): ResponseEntity<RdpResultWrapper<Nothing>> {
        return json(
            HttpStatus.BAD_REQUEST,
            RdpResultWrapper(RdpResultWrapper.CODE_INVALID_PARAMETER, "요청 본문이 올바르지 않습니다")
        )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<RdpResultWrapper<Nothing>> {
        return json(ex.httpStatus, RdpResultWrapper(ex.errorCode, ex.message))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<RdpResultWrapper<Nothing>> {
        return json(HttpStatus.FORBIDDEN, RdpResultWrapper(RdpResultWrapper.CODE_INSUFFICIENT_SCOPE, "권한 없음"))
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<RdpResultWrapper<Nothing>> {
        return json(
            HttpStatus.INTERNAL_SERVER_ERROR,
            RdpResultWrapper(RdpResultWrapper.CODE_INTERNAL_ERROR, "내부 오류")
        )
    }
}
