package com.otoki.powersales.domain.activity.claim.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class InvalidClaimType1Exception : BusinessException(
    errorCode = "INVALID_CLAIM_TYPE1",
    message = "유효하지 않은 클레임 대분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidClaimType2Exception : BusinessException(
    errorCode = "INVALID_CLAIM_TYPE2",
    message = "유효하지 않은 클레임 소분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * SF dependent picklist (`DKRetail__ClaimType2__c.controllerName == "DKRetail__ClaimType1__c"`) 의
 * validFor 비트맵 기반 매핑 위반 (예: claimType1=A + claimType2=BA).
 */
class ClaimTypeHierarchyMismatchException : BusinessException(
    errorCode = "CLAIM_TYPE_HIERARCHY_MISMATCH",
    message = "클레임 소분류가 선택한 대분류에 속하지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidPurchaseMethodException : BusinessException(
    errorCode = "INVALID_PURCHASE_METHOD",
    message = "유효하지 않은 구매방법입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidRequestTypeException : BusinessException(
    errorCode = "INVALID_REQUEST_TYPE",
    message = "유효하지 않은 요청유형입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidDateTypeException : BusinessException(
    errorCode = "INVALID_DATE_TYPE",
    message = "유효하지 않은 기한 종류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ClaimInvalidParameterException(detail: String) : BusinessException(
    errorCode = "CLAIM_INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * "임시저장" 상태가 아닌 클레임에 수정/삭제 시도. 레거시 Trigger 룰 정합:
 * "임시저장일 경우에만 수정이 가능합니다!" / "임시저장일 경우에만 삭제가 가능합니다!"
 */
class ClaimNotEditableException : BusinessException(
    errorCode = "CLAIM_NOT_EDITABLE",
    message = "임시저장 상태의 클레임만 수정/삭제할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ClaimAccessDeniedException : BusinessException(
    errorCode = "CLAIM_ACCESS_DENIED",
    message = "해당 클레임에 대한 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class ClaimPhotoNotFoundException(photoId: Long) : BusinessException(
    errorCode = "CLAIM_PHOTO_NOT_FOUND",
    message = "사진을 찾을 수 없습니다: $photoId",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 레거시 SF Validation Rule (RequestTypeRule) 정합:
 * "요청사항은 최대 4개까지 선택해주세요."
 */
class RequestTypeMaxExceededException : BusinessException(
    errorCode = "REQUEST_TYPE_MAX_EXCEEDED",
    message = "요청사항은 최대 4개까지 선택해주세요",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 레거시 ClaimTrigger 정합:
 * "제조일자를 다시한번 확인해주십시오." / "발생일자를 다시한번 확인해주십시오."
 */
class InvalidClaimDateException(message: String) : BusinessException(
    errorCode = "INVALID_CLAIM_DATE",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 영수증 사진 조건부 필수 위반 (Spec #829).
 * purchaseMethod ∈ {B(개인카드), C(현금)} 인 경우 영수증 사진이 필수.
 */
class ReceiptRequiredException : BusinessException(
    errorCode = "RECEIPT_REQUIRED",
    message = "개인카드/현금 구매 시 영수증 사진은 필수입니다",
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY
)

/**
 * SF 재전송 endpoint 가 SEND_FAILED 상태가 아닌 클레임에 호출됐을 때 (Spec #829).
 */
class ClaimNotResendableException : BusinessException(
    errorCode = "CLAIM_NOT_RESENDABLE",
    message = "전송실패 상태에서만 SF 재전송할 수 있습니다",
    httpStatus = HttpStatus.CONFLICT
)
