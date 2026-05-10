package com.otoki.powersales.common.service

import com.otoki.powersales.common.exception.InvalidFileException
import com.otoki.powersales.common.storage.StorageService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class FileStorageService(
	private val storageService: StorageService
) {

	/**
	 * 일매출 사진 업로드. 신규 객체는 S3 키 형식(`uploads/daily-sales/<yyyy>/<mm>/<dd>/<uuid>.<ext>`)으로 저장된다.
	 */
	fun uploadDailySalesPhoto(
		file: MultipartFile,
		userId: Long,
		eventId: String,
		salesDate: String
	): String {
		validateNotEmpty(file)
		val result = storageService.upload(
			domain = "daily-sales",
			originalName = file.originalFilename ?: "unknown",
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		return result.key
	}

	/**
	 * 공지사항 첨부 이미지 업로드. 신규 객체는 S3 키 형식(`uploads/notice/<yyyy>/<mm>/<dd>/<uuid>.<ext>`)으로 저장된다.
	 */
	fun uploadNoticeImage(file: MultipartFile, noticeId: Long): String {
		validateNotEmpty(file)
		val result = storageService.upload(
			domain = "notice",
			originalName = file.originalFilename ?: "unknown",
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		return result.key
	}

	/**
	 * 교육 자료 파일 업로드. 신규 객체는 S3 키 형식(`uploads/education/<yyyy>/<mm>/<dd>/<uuid>.<ext>`)으로 저장된다.
	 */
	fun uploadEducationFile(file: MultipartFile, eduId: String): String {
		validateNotEmpty(file)
		val result = storageService.upload(
			domain = "education",
			originalName = file.originalFilename ?: "unknown",
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		return result.key
	}

	/**
	 * 교육 자료 파일 삭제. fileKey 가 신규 S3 키이면 그대로 삭제, 레거시 UUID 형식이면 no-op (범위 외).
	 */
	fun deleteEducationFile(eduId: String, fileKey: String) {
		if (fileKey.startsWith("uploads/")) {
			storageService.delete(fileKey)
		}
	}

	private fun validateNotEmpty(file: MultipartFile) {
		if (file.isEmpty) throw InvalidFileException("빈 파일은 업로드할 수 없습니다")
		if (file.originalFilename.isNullOrBlank()) throw InvalidFileException("파일명이 올바르지 않습니다")
	}
}
