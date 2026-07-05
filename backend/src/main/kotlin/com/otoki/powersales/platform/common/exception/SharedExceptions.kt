package com.otoki.powersales.platform.common.exception

import org.springframework.http.HttpStatus

// 공유 예외 클래스 — 원래 OrderDraftExceptions.kt, InspectionExceptions.kt 등에 정의되었으나
// 해당 파일이 주석 처리됨에 따라 활성 코드에서 참조하는 예외를 분리 (Spec 69).
// 또한 여러 도메인이 공유하는 신규 공통 예외(예: prod 기능 게이트)도 여기에 둔다.

class InvalidOrderParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_ORDER_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class ProductNotFoundException(productCode: String? = null) : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = if (productCode != null) "제품을 찾을 수 없습니다: $productCode" else "제품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class AlreadyFavoritedException : BusinessException(
    errorCode = "ALREADY_FAVORITED",
    message = "이미 즐겨찾기에 추가된 제품입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class FavoriteNotFoundException : BusinessException(
    errorCode = "FAVORITE_NOT_FOUND",
    message = "즐겨찾기에 없는 제품입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 파일 저장 오류
 */
class FileStorageException(
    detail: String,
    cause: Throwable? = null
) : BusinessException(
    errorCode = "FILE_STORAGE_ERROR",
    message = detail,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause = cause
)

/**
 * 유효하지 않은 파일
 */
class InvalidFileException(detail: String) : BusinessException(
    errorCode = "INVALID_FILE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class AccountInvalidParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 운영(prod) 환경에서 아직 비활성인 기능의 등록 시도 차단.
 *
 * 주문 등록 / 주문(제품) 클레임 등록 / 물류 클레임 등록은 SF dual-write 연동을 포함하는데,
 * 운영 환경에서는 관련 부서 협의 전까지 해당 등록을 열지 않는다. prod 프로파일에서만 발동하며
 * dev/local 은 영향받지 않는다. [com.otoki.powersales.platform.common.config.ProdFeatureGate] 참조.
 */
class FeatureNotYetEnabledException : BusinessException(
    errorCode = "FEATURE_NOT_YET_ENABLED",
    message = "관련 부서 협의 후, 활성화 예정입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
