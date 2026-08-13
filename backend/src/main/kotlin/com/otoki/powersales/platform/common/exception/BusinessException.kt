package com.otoki.powersales.platform.common.exception

import org.springframework.http.HttpStatus

/**
 * 비즈니스 로직 예외.
 * 도메인 전반에서 상속받아 사용하므로 별도 파일로 둔다 (GlobalExceptionHandler 와 같은 파일에 두면
 * 핸들러 추가 시 forward reference 로 컴파일 cycle 이 발생).
 *
 * [serverFault] — 로그 레벨 판정용. `true` 면 `GlobalExceptionHandler` 가 스택트레이스까지 `error` 로
 * 남기고, `false` 면 `warn` 요약만 남긴다. 기본값은 HTTP status 기반(5xx = 서버 결함)이라 기존 예외는
 * 동작 변화가 없다. **5xx 이지만 서버 결함이 아닌** 케이스 — 외부 시스템이 정상 응답으로 업무 규칙상
 * 요청을 거부한 경우(예: SAP 가 "판매문서 릴리즈중" 으로 취소를 거절) — 만 `false` 로 명시 선언한다.
 * 그런 케이스는 사용자 재시도로 해소되는 예상된 흐름이라, 스택트레이스가 진단에 기여하지 않고 실제
 * 서버 결함 로그를 묻어버리는 노이즈가 된다.
 */
open class BusinessException(
    val errorCode: String,
    override val message: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    override val cause: Throwable? = null,
    val serverFault: Boolean = httpStatus.is5xxServerError,
) : RuntimeException(message, cause)
