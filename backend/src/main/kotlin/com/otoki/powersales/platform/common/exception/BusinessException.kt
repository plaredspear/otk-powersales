package com.otoki.powersales.platform.common.exception

import org.springframework.http.HttpStatus

/**
 * 비즈니스 로직 예외.
 * 도메인 전반에서 상속받아 사용하므로 별도 파일로 둔다 (GlobalExceptionHandler 와 같은 파일에 두면
 * 핸들러 추가 시 forward reference 로 컴파일 cycle 이 발생).
 */
open class BusinessException(
    val errorCode: String,
    override val message: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
