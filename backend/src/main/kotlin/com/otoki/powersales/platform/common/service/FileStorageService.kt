package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.exception.InvalidFileException
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
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
	 * 교육 자료 파일 업로드. 실제 S3 key = `private/education/<fileKey>` 에 저장되며, 반환값(= DB `file_key`)은
	 * segment 없는 `<fileKey>` 원본이다. 레거시 마이그레이션분과 동일한 평면 file_key 규칙을 유지한다:
	 * `{13자리 epoch밀리초}{8자리 random}.{ext}` (최대 25자, `file_key` 컬럼 length=30 이내).
	 * (레거시는 public S3 URL 이었으나 권한 통제 정합 위해 다른 도메인과 동일하게 private 전환.)
	 */
	fun uploadEducationFile(file: MultipartFile, eduId: String): String {
		validateNotEmpty(file)
		val fileKey = newEducationFileKey(file.originalFilename ?: "unknown")
		val result = storageService.uploadPrivateWithKey(
			uniqueKey = educationUniqueKey(fileKey),
			bytes = file.bytes,
			contentType = file.contentType ?: throw InvalidFileException("파일 타입을 확인할 수 없습니다")
		)
		// uploadPrivateWithKey 반환 key 는 넘긴 uniqueKey("education/<fileKey>") 이므로 segment 를 벗겨 file_key 로 저장.
		return result.key.substringAfterLast('/')
	}

	/**
	 * 교육 자료 파일 조회용 presigned URL. 교육 첨부는 `private/education/<fileKey>` 에 저장되며(private 세그먼트는
	 * StorageService 가 합성), DB `file_key` 는 segment 없는 원본값(레거시 `161733...jpg`)을 그대로 보관한다.
	 * 여기서 `education/` 도메인 prefix 만 붙여 uniqueKey 를 만들면 실제 S3 key = `private/education/<fileKey>` 가 된다.
	 */
	fun getEducationFileUrl(fileKey: String): String =
		storageService.getPresignedUrl(educationUniqueKey(fileKey), StorageConstants.EDUCATION_PRESIGN_TTL_SECONDS)

	/** DB 보관 `file_key`(segment 없음) → private 도메인 uniqueKey. 실제 S3 key = `private/` + 반환값. */
	private fun educationUniqueKey(fileKey: String): String = "education/$fileKey"

	/** 신규 교육 첨부 file_key 생성. 레거시 형식과 호환되는 평면 timestamp 기반, 컬럼 length=30 이내. */
	private fun newEducationFileKey(originalName: String): String {
		val ext = originalName.substringAfterLast('.', "").lowercase()
		val rand = "%08d".format(kotlin.random.Random.nextInt(0, 100_000_000))
		val base = "${System.currentTimeMillis()}$rand"
		return if (ext.isNotBlank()) "$base.$ext" else base
	}

	/**
	 * 교육 자료 파일 삭제. DB `file_key`(segment 없음)를 받아 `private/education/<fileKey>` 객체를 삭제한다.
	 * deletePrivate 는 NoSuchKey 를 idempotent 로 무시하므로 레거시/신규 구분 없이 안전하다.
	 */
	fun deleteEducationFile(eduId: String, fileKey: String) {
		storageService.deletePrivate(educationUniqueKey(fileKey))
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
