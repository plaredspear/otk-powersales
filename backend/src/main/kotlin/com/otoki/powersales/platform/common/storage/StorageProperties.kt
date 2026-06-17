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
	val DEFAULT_PRESIGN_TTL: Duration = Duration.ofMinutes(5)
}
