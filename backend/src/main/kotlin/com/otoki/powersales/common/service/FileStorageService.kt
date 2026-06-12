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
	 * 일매출 마감 사진 업로드. 일매출 마감 데이터는 "본인 PromotionEmployee 만 조회"로 권한 통제되므로
	 * 현장점검/클레임과 동일하게 S3 private/ 폴더에 저장한다(조회 시 presigned URL). 반환 key(= DB s3ImageUniqueKey)는
	 * segment 없는 `uploads/daily-sales/<yyyy>/<mm>/<dd>/<uuid>.<ext>` 형식이며, 실제 S3 객체는
	 * `private/` + key 에 위치한다. (레거시는 public S3 URL 이었으나, 권한 통제 정합 위해 private 전환.)
	 */
	fun uploadDailySalesPhoto(
		file: MultipartFile,
		userId: Long,
		eventId: String,
		salesDate: String
	): String {
		validateNotEmpty(file)
		val result = storageService.uploadPrivate(
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
	/**
	 * 공지 첨부/본문 이미지 업로드. 권한 통제 대상이므로 S3 private/ 폴더에 저장된다(조회 시 presigned URL).
	 * 반환 key(= DB uniqueKey)는 segment 없는 `uploads/notice/<yyyy>/<mm>/<dd>/<uuid>.<ext>` 형식이며,
	 * 실제 S3 객체는 `private/` + key 에 위치한다. (레거시 공지 첨부는 public 이었으나, 본문 인라인 이미지의
	 * private 전환과 일관되게 첨부도 private 로 통일 — Spec #854 재설계.)
	 */
	fun uploadNoticeImage(file: MultipartFile, noticeId: Long): String {
		validateNotEmpty(file)
		val result = storageService.uploadPrivate(
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

	/**
	 * 클레임 사진 업로드. 권한 통제 대상이므로 S3 private/ 폴더에 저장된다(조회 시 presigned URL).
	 * 반환 key(= DB uniqueKey)는 segment 없는 `uploads/claim/<yyyy>/<mm>/<dd>/<uuid>.<ext>` 형식이며,
	 * 실제 S3 객체는 `private/` + key 에 위치한다.
	 */
	fun uploadClaimPhoto(file: MultipartFile, userId: Long, claimId: Long, photoType: String): String {
		validateNotEmpty(file)
		val result = storageService.uploadPrivate(
			domain = "claim",
			originalName = file.originalFilename ?: "unknown",
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		return result.key
	}

	/**
	 * 제안(물류 클레임) 첨부 사진 업로드 (Spec #664). 권한 통제 대상이므로 제품 클레임과 동일하게 S3 private/ 폴더에
	 * 저장된다(조회 시 presigned URL). 반환 key(= DB uniqueKey)는 segment 없는
	 * `uploads/suggestion/<yyyy>/<mm>/<dd>/<uuid>.<ext>` 형식이며, 실제 S3 객체는 `private/` + key 에 위치한다.
	 */
	fun uploadSuggestionPhoto(file: MultipartFile, suggestionId: Long): String {
		validateNotEmpty(file)
		val result = storageService.uploadPrivate(
			domain = "suggestion",
			originalName = file.originalFilename ?: "unknown",
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		return result.key
	}

	/**
	 * 제안(물류 클레임) 첨부 사진 삭제 (Spec #828). private/ 객체로 저장되므로 deletePrivate 로 삭제한다
	 * (실 객체 key = private/ + uniqueKey). 레거시 마이그레이션 키(segment·uploads 미포함)도 private/ 합성으로 처리.
	 */
	fun deleteSuggestionPhoto(fileKey: String) {
		storageService.deletePrivate(fileKey)
	}

	/**
	 * 현장점검(site-activity) 사진 업로드. 현장점검 데이터는 "본인만 조회"로 권한 통제되므로
	 * 클레임/제안과 동일하게 S3 private/ 폴더에 저장한다(조회 시 presigned URL). 반환 key(= DB uniqueKey)는
	 * segment 없는 `uploads/site-activity/<yyyy>/<mm>/<dd>/<uuid>.<ext>` 형식이며, 실제 S3 객체는
	 * `private/` + key 에 위치한다. (레거시는 public S3 URL 이었으나, 권한 통제 정합 위해 private 전환.)
	 */
	fun uploadSiteActivityPhoto(file: MultipartFile, siteActivityId: Long): String {
		validateNotEmpty(file)
		val result = storageService.uploadPrivate(
			domain = "site-activity",
			originalName = file.originalFilename ?: "unknown",
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		return result.key
	}

	/**
	 * 현장점검 사진 바이트 다운로드 (엑셀 export 용). private/ 객체이므로 downloadPrivate 로 읽는다
	 * (실 객체 key = private/ + uniqueKey).
	 */
	fun downloadSiteActivityPhoto(uniqueKey: String): ByteArray =
		storageService.downloadPrivate(uniqueKey)

	private fun validateNotEmpty(file: MultipartFile) {
		if (file.isEmpty) throw InvalidFileException("빈 파일은 업로드할 수 없습니다")
		if (file.originalFilename.isNullOrBlank()) throw InvalidFileException("파일명이 올바르지 않습니다")
	}
}
