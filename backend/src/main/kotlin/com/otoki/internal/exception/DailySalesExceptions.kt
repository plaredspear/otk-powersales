package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 일매출 필수 입력 항목 누락
 * 대표제품 또는 기타제품 중 최소 하나를 입력해야 함
 */
class DailySalesInvalidParameterException(
    message: String = "필수 입력 항목이 누락되었습니다"
) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 일매출 사진 파일 관련 오류
 * - 사진 미첨부
 * - 파일 크기 초과 (10MB)
 * - 파일 형식 오류 (JPEG, PNG만 허용)
 */
class DailySalesInvalidPhotoException(
    message: String = "사진 파일이 필요합니다"
) : BusinessException(
    errorCode = "INVALID_PHOTO",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 일매출 유효하지 않은 제품
 * 행사 제품 목록에 없는 제품 코드
 */
class DailySalesInvalidProductException(
    message: String = "유효하지 않은 제품입니다"
) : BusinessException(
    errorCode = "INVALID_PRODUCT",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 일매출 중복 등록
 * 동일 행사, 동일 사원, 동일 날짜에 이미 REGISTERED 상태의 일매출이 존재
 */
class DailySalesAlreadyRegisteredException(
    message: String = "오늘 매출이 이미 등록되었습니다"
) : BusinessException(
    errorCode = "ALREADY_REGISTERED",
    message = message,
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 행사 기간 만료
 * 현재 날짜가 행사 기간(startDate ~ endDate) 밖인 경우
 */
class EventPeriodExpiredException(
    message: String = "행사 기간이 아닙니다"
) : BusinessException(
    errorCode = "EVENT_PERIOD_EXPIRED",
    message = message,
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY
)

/**
 * 일매출 등록 권한 없음
 * 행사 담당자가 아닌 사용자가 등록 시도
 */
class DailySalesForbiddenException(
    message: String = "등록 권한이 없습니다"
) : BusinessException(
    errorCode = "FORBIDDEN",
    message = message,
    httpStatus = HttpStatus.FORBIDDEN
)
