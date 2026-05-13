/*
package com.otoki.powersales.claim.exception

import com.otoki.powersales.common.exception.BusinessException

import org.springframework.http.HttpStatus

/ **
 * 유효하지 않은 클레임 대분류 (ClaimType1)
 * /
class InvalidClaimType1Exception : BusinessException(
    errorCode = "INVALID_CLAIM_TYPE1",
    message = "유효하지 않은 클레임 대분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 유효하지 않은 클레임 소분류 (ClaimType2)
 * /
class InvalidClaimType2Exception : BusinessException(
    errorCode = "INVALID_CLAIM_TYPE2",
    message = "유효하지 않은 클레임 소분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 클레임 대분류-소분류 계층 불일치
 *
 * SF dependent picklist (`DKRetail__ClaimType2__c.controllerName == "DKRetail__ClaimType1__c"`) 의
 * validFor 비트맵 기반 매핑 위반 (예: claimType1=A + claimType2=BA).
 * /
class ClaimTypeHierarchyMismatchException : BusinessException(
    errorCode = "CLAIM_TYPE_HIERARCHY_MISMATCH",
    message = "클레임 소분류가 선택한 대분류에 속하지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 구매 방법을 찾을 수 없음
 * /
class PurchaseMethodNotFoundException : BusinessException(
    errorCode = "PURCHASE_METHOD_NOT_FOUND",
    message = "구매 방법을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/ **
 * 요청사항을 찾을 수 없음
 * /
class RequestTypeNotFoundException : BusinessException(
    errorCode = "REQUEST_TYPE_NOT_FOUND",
    message = "요청사항을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/ **
 * 유효하지 않은 기한 종류
 * /
class InvalidDateTypeException : BusinessException(
    errorCode = "INVALID_DATE_TYPE",
    message = "유효하지 않은 기한 종류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 구매 정보 불완전
 * /
class PurchaseInfoRequiredException : BusinessException(
    errorCode = "PURCHASE_INFO_REQUIRED",
    message = "구매 금액 입력 시 구매 방법과 영수증 사진은 필수입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 유효하지 않은 파라미터
 * /
class InvalidParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)
*/
