package com.otoki.powersales.domain.support.notice.exception

import com.otoki.powersales.platform.common.exception.BusinessException

import org.springframework.http.HttpStatus

/**
 * 유효하지 않은 공지사항 카테고리
 */
class InvalidNoticeCategoryException : BusinessException(
    errorCode = "INVALID_CATEGORY",
    message = "유효하지 않은 분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 공지사항 공개범위
 */
class InvalidNoticeScopeException : BusinessException(
    errorCode = "INVALID_SCOPE",
    message = "유효하지 않은 공개범위입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 공지사항 게시물을 찾을 수 없음
 */
class NoticePostNotFoundException : BusinessException(
    errorCode = "NOTICE_NOT_FOUND",
    message = "공지사항을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 유효하지 않은 공지사항 ID
 */
class InvalidNoticeIdException : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = "유효하지 않은 요청입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class BranchRequiredException : BusinessException(
    errorCode = "BRANCH_REQUIRED",
    message = "지점공지는 지점 정보가 필요합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 조장/지점장 권한은 지점공지(BRANCH) 외 카테고리로 공지를 등록/수정할 수 없다.
 * (프론트 UI 는 지점공지만 노출하나, API 직접 호출 우회를 서버에서도 차단)
 */
class BranchNoticeOnlyException : BusinessException(
    errorCode = "BRANCH_NOTICE_ONLY",
    message = "지점공지만 작성할 수 있습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 발행되지 않은(임시저장) 공지는 push 발송할 수 없다.
 * 임시저장 공지는 모바일에 노출되지 않으므로 push 대상이 될 수 없다.
 */
class NoticeNotPublishedException : BusinessException(
    errorCode = "NOTICE_NOT_PUBLISHED",
    message = "발행된 공지사항만 푸시 발송할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 영업사원 공개범위(scope) 공지는 모바일에 노출되지 않으므로 push 발송할 수 없다.
 * (조회 노출 규칙과 정합 — 영업사원 scope 는 앱 목록에서 제외됨)
 */
class NoticeScopeNotPushableException : BusinessException(
    errorCode = "NOTICE_SCOPE_NOT_PUSHABLE",
    message = "이 공개범위의 공지사항은 푸시 발송 대상이 아닙니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 첨부 이미지 ID — imageId 미존재 또는 parent 불일치
 */
class InvalidImageIdException : BusinessException(
    errorCode = "INVALID_IMAGE_ID",
    message = "유효하지 않은 이미지 ID입니다",
    httpStatus = HttpStatus.NOT_FOUND
)
