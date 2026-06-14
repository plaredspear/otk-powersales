package com.otoki.powersales.savedsearch.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class SavedSearchNotFoundException : BusinessException(
    errorCode = "SAVED_SEARCH_NOT_FOUND",
    message = "저장된 검색을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)

class SavedSearchForbiddenException : BusinessException(
    errorCode = "SAVED_SEARCH_FORBIDDEN",
    message = "저장된 검색에 대한 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN,
)

class SavedSearchDuplicateNameException : BusinessException(
    errorCode = "SAVED_SEARCH_DUPLICATE_NAME",
    message = "같은 이름의 검색이 이미 있습니다",
    httpStatus = HttpStatus.CONFLICT,
)
