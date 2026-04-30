package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

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
        SapSalesHistoryController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SapInboundExceptionHandler {

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

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<SapResultWrapper<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(SapResultWrapper(SapResultWrapper.CODE_INTERNAL_ERROR, "내부 오류"))
    }
}
