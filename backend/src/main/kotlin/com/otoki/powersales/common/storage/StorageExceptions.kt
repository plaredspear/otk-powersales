package com.otoki.powersales.common.storage

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class StorageWriteFailedException(reason: String? = null, cause: Throwable? = null) : BusinessException(
	errorCode = "STORAGE_WRITE_FAILED",
	message = reason ?: "스토리지 저장에 실패했습니다",
	httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
	cause = cause
)

class StorageNotFoundException(key: String) : BusinessException(
	errorCode = "STORAGE_NOT_FOUND",
	message = "객체를 찾을 수 없습니다: $key",
	httpStatus = HttpStatus.NOT_FOUND
)

class UnsupportedMediaTypeException(contentType: String?) : BusinessException(
	errorCode = "UNSUPPORTED_MEDIA_TYPE",
	message = "지원하지 않는 파일 형식입니다: ${contentType ?: "알 수 없음"}",
	httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE
)

class FileTooLargeException(sizeBytes: Long, limitBytes: Long) : BusinessException(
	errorCode = "FILE_TOO_LARGE",
	message = "파일 크기가 제한을 초과했습니다 (${sizeBytes} bytes, 한도 ${limitBytes} bytes)",
	httpStatus = HttpStatus.PAYLOAD_TOO_LARGE
)
