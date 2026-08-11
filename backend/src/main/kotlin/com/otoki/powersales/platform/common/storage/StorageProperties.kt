package com.otoki.powersales.platform.common.storage

import java.time.Duration

object StorageConstants {
	const val MAX_FILE_BYTES: Long = 20L * 1024 * 1024

	// private/ 객체의 실제 S3 key 합성 규칙. 버킷 정책: public/ 만 anonymous read 허용, private/ 는 차단.
	// (public 객체의 가시성 prefix 는 S3_PUBLIC_URL_PREFIX 가 URL 단에서 담당하므로 별도 segment 상수 불요.)
	// DB UploadFile.uniqueKey 에는 segment 를 포함하지 않고(= uploads/...), StorageService 레이어가
	// 실제 S3 연산(PUT/GET/DELETE/presign) 시점에 이 helper 로 합성한다 (S3/Local impl 공통 SoT).
	private const val PRIVATE_SEGMENT: String = "private"

	fun privateKey(uniqueKey: String): String = "$PRIVATE_SEGMENT/$uniqueKey"

	// 제품 클레임 이미지 presigned URL 만료 시간(초). 목록/상세 공통. 짧은 세션 내 소비 + 클라이언트
	// 자동 복구로 만료 마찰을 흡수하므로 보수적으로 10분.
	const val CLAIM_PRESIGN_TTL_SECONDS: Int = 600

	// 공지 본문 인라인 이미지 + 첨부 이미지 presigned URL 만료 시간(초). 본문은 긴 글을 천천히 스크롤하며
	// 읽으므로 클레임(600s)보다 길게 30분. web 은 페이지 로드 시 1회 소비, mobile 은 cacheKey(refid)로
	// 재요청을 흡수하므로 만료 마찰이 작다.
	const val NOTICE_PRESIGN_TTL_SECONDS: Int = 1800

	// 교육자료 첨부(이미지/동영상/문서) presigned URL 만료 시간(초). 동영상은 천천히 재생되므로
	// 공지(1800s)와 동일하게 30분. (교육 파일은 public 경로 객체이나 anonymous read 미허용이라 presign 한다.)
	const val EDUCATION_PRESIGN_TTL_SECONDS: Int = 1800

	// 현장점검(site-activity) 사진 presigned URL 만료 시간(초). 상세 화면 1회 조회 소비 + 클라이언트
	// 새로고침으로 만료를 흡수하므로 클레임과 동일하게 10분. (엑셀 export 는 만료 회피 위해 URL 이 아닌
	// 이미지 바이트를 임베드하므로 이 TTL 과 무관.)
	const val SITE_ACTIVITY_PRESIGN_TTL_SECONDS: Int = 600

	// 일매출(daily-sales) 마감 사진 presigned URL 만료 시간(초). 마감 폼 1회 조회 소비 + 새로고침 흡수로
	// 현장점검과 동일하게 10분. (사진은 마감 필수값, PromotionEmployee.s3ImageUniqueKey 단일 보관.)
	const val DAILY_SALES_PRESIGN_TTL_SECONDS: Int = 600

	// 모바일 앱 패키지(APK/IPA) 최대 크기(초). 이미지(20MB)와 달리 수십~수백 MB 바이너리이므로 별도 상한.
	const val APP_PACKAGE_MAX_BYTES: Long = 500L * 1024 * 1024

	// 앱 패키지 다운로드 presigned URL 만료 시간(초). iOS OTA 는 manifest fetch → 사용자 확인 →
	// IPA 다운로드까지 수 분 소요되므로(설치 시작 시점부터 TTL 카운트) 넉넉히 15분. Android 도 대용량 APK
	// 다운로드를 고려해 동일 적용.
	const val APP_PACKAGE_PRESIGN_TTL_SECONDS: Int = 900

	val ALLOWED_CONTENT_TYPES: Set<String> = setOf(
		"image/jpeg",
		"image/png",
		"image/heic",
		"application/pdf",
		"image/jpg",
		"image/gif",
		"image/webp",
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"application/vnd.ms-excel"
	)

	// 교육 자료 첨부 최대 크기. 첨부가 동영상(APP 매뉴얼 등)까지 포함하므로 이미지 기준(20MB)과 별도 상한을 둔다.
	// web/mobile 이 안내하는 "개별 50MB 이하" 와 동일 값 (spring.servlet.multipart.max-file-size=512MB 이내).
	const val EDUCATION_MAX_FILE_BYTES: Long = 50L * 1024 * 1024

	// 교육 자료 첨부 허용 content-type. 다른 도메인(클레임/공지/현장점검)은 이미지 전용이므로
	// ALLOWED_CONTENT_TYPES 에 동영상/문서를 넣지 않고 교육 전용 집합으로 분리한다.
	// 확장자 → 파일 유형 코드(f00001 이미지 / f00002 동영상 / f00003 문서) 매핑과 동일 범위를 커버한다
	// (EducationService.determineFileType). 브라우저/OS 별로 같은 확장자에 다른 MIME 을 실어 보내므로
	// (예: .wmv → video/x-ms-wmv, .mkv → video/x-matroska) 관용 변형까지 함께 허용한다.
	val EDUCATION_ALLOWED_CONTENT_TYPES: Set<String> = ALLOWED_CONTENT_TYPES + setOf(
		// 동영상 (f00002): mp4 / avi / wmv / mkv / mov / m4v
		"video/mp4",
		"video/x-m4v",
		"video/quicktime",
		"video/x-msvideo",
		"video/avi",
		"video/x-ms-wmv",
		"video/x-matroska",
		// 문서 (f00003): docx / txt / hwp / pptx (pdf / xlsx 는 ALLOWED_CONTENT_TYPES 에 이미 포함)
		"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		"application/msword",
		"text/plain",
		"application/haansofthwp",
		"application/x-hwp",
		"application/vnd.hancom.hwp",
		"application/vnd.openxmlformats-officedocument.presentationml.presentation",
		"application/vnd.ms-powerpoint"
	)

	// 브라우저/OS 가 content-type 을 못 붙였다는 신호. 빈 값 외에 octet-stream 도 "모르겠음"의 관용 표현이라
	// 같이 취급한다 (.hwp 처럼 OS MIME 레지스트리에 없는 확장자에서 실제로 관측된다).
	private val UNKNOWN_CONTENT_TYPES: Set<String> = setOf("", "application/octet-stream")

	// 확장자 → content-type fallback. 위 UNKNOWN_CONTENT_TYPES 인 업로드에 한해 확장자로 MIME 을 보정한다.
	// key 집합은 EducationService.determineFileType 의 f00001/f00002/f00003 확장자 목록과 1:1 로 맞춘다 —
	// 여기 없는 확장자는 보정하지 않고 그대로 거부되므로, 매핑 누락이 곧 화이트리스트 우회로 이어지지 않는다.
	private val EDUCATION_EXTENSION_CONTENT_TYPES: Map<String, String> = mapOf(
		// f00001 이미지
		"jpg" to "image/jpeg",
		"jpeg" to "image/jpeg",
		"png" to "image/png",
		"gif" to "image/gif",
		// f00002 동영상
		"mp4" to "video/mp4",
		"m4v" to "video/x-m4v",
		"mov" to "video/quicktime",
		"avi" to "video/x-msvideo",
		"wmv" to "video/x-ms-wmv",
		"mkv" to "video/x-matroska",
		// f00003 문서
		"pdf" to "application/pdf",
		"docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		"txt" to "text/plain",
		"hwp" to "application/x-hwp",
		"pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
		"xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	)

	/**
	 * 교육 첨부의 실효 content-type 을 판정한다. 업로드가 content-type 을 실어 왔으면 그대로 쓰고, 비어 있거나
	 * `application/octet-stream` 이면 파일명 확장자로 보정한다 (보정 실패 시 null → 호출자가 거부).
	 *
	 * 보정된 값도 [EDUCATION_ALLOWED_CONTENT_TYPES] 검증을 그대로 통과해야 하므로, 이 fallback 이 화이트리스트를
	 * 넓히지는 않는다. 확장자를 신뢰하는 범위는 위 매핑 테이블에 등재된 것으로 한정된다.
	 */
	fun resolveEducationContentType(contentType: String?, originalName: String?): String? {
		val declared = contentType?.trim()?.lowercase().orEmpty()
		if (declared !in UNKNOWN_CONTENT_TYPES) return declared
		val ext = originalName?.substringAfterLast('.', "")?.lowercase().orEmpty()
		return EDUCATION_EXTENSION_CONTENT_TYPES[ext]
	}

	val DEFAULT_PRESIGN_TTL: Duration = Duration.ofMinutes(5)
}
