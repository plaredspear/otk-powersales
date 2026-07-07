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
 * 지점공지 등록/수정 시 선택한 지점이 작성자의 권한 스코프(WomenScheduleBranchResolver 화이트리스트) 밖인 경우.
 * 프론트 드롭다운에 없는 지점코드를 API 직접 호출로 심는 우회(IDOR)를 차단한다.
 */
class BranchNotAllowedException : BusinessException(
    errorCode = "BRANCH_NOT_ALLOWED",
    message = "선택할 수 없는 지점입니다",
    httpStatus = HttpStatus.FORBIDDEN
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

/**
 * 공지 동시 수정 충돌 — 수정 화면을 연 뒤 다른 사용자가 먼저 저장해 버전이 어긋난 경우.
 * 나중 저장자의 요청을 거부해 lost update(먼저 저장자의 변경 덮어쓰기) + 인라인 이미지 교차 오삭제를 막는다.
 * 클라이언트는 이 응답을 받으면 "다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러오세요" 안내를 띄운다.
 */
class NoticeVersionConflictException : BusinessException(
    errorCode = "NOTICE_VERSION_CONFLICT",
    message = "다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 저장해주세요",
    httpStatus = HttpStatus.CONFLICT
)
